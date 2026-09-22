package br.com.organizadorfinanceiro.imports.itau;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.organizadorfinanceiro.imports.ImportDocumentStatus;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ItauCardXlsxParserTest {
    private final ItauCardXlsxParser parser = new ItauCardXlsxParser();

    @Test
    void detectsPaidOpenAndProjectedStatusesFromWorkbookTitle() throws Exception {
        assertThat(parse("Fatura Paga - Setembro/2099", 80.00).documentStatus())
                .isEqualTo(ImportDocumentStatus.PAID);
        assertThat(parse("Fatura Aberta - Setembro/2099", 80.00).documentStatus())
                .isEqualTo(ImportDocumentStatus.OPEN);
        assertThat(parse("Fatura Próxima - Setembro/2099", 80.00).documentStatus())
                .isEqualTo(ImportDocumentStatus.PROJECTED);
    }

    @Test
    void parsesInstallmentsAndReconcilesTotalWithoutStatementPayment() throws Exception {
        ItauCardStatement statement = parse("Fatura Paga - Setembro/2099", 80.00);

        assertThat(statement.competence().toString()).isEqualTo("2099-09");
        assertThat(statement.dueDate()).isEqualTo(LocalDate.of(2099, 9, 10));
        assertThat(statement.masterCardLastFourDigits()).isEqualTo("5767");
        assertThat(statement.entries()).hasSize(3);
        assertThat(statement.entries().get(0).statementPayment()).isTrue();
        assertThat(statement.entries().get(1).installmentNumber()).isEqualTo(2);
        assertThat(statement.entries().get(1).installmentTotal()).isEqualTo(10);
        assertThat(statement.entries().get(1).usedCardLastFourDigits()).isEqualTo("5376");
        assertThat(statement.entries().get(2).signedAmount()).isEqualByComparingTo("-20.00");
    }

    @Test
    void rejectsWorkbookWhoseDisplayedTotalDoesNotReconcile() {
        assertThatThrownBy(() -> parse("Fatura Aberta - Setembro/2099", 81.00))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não reconcilia");
    }

    private ItauCardStatement parse(String title, double total) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Fatura 09-99");
            sheet.createRow(7).createCell(1).setCellValue(title);
            Row labels = sheet.createRow(8);
            labels.createCell(1).setCellValue("Cartão");
            labels.createCell(6).setCellValue("Valor (parcial)");
            labels.createCell(8).setCellValue("Vencimento");
            Row metadata = sheet.createRow(9);
            metadata.createCell(1).setCellValue("Latam Pass Itau Platinum Mastercard - final 5767");
            metadata.createCell(6).setCellValue(total);
            metadata.createCell(8).setCellValue(DateUtil.getExcelDate(LocalDateTime.of(2099, 9, 10, 0, 0)));
            Row header = sheet.createRow(13);
            String[] names = {"Data", "Lançamento", "Parcelamento", "Valor", "Titularidade",
                    "Nome", "Tipo do cartão", "Número do cartão"};
            int[] columns = {1, 2, 3, 4, 6, 7, 8, 9};
            for (int index = 0; index < names.length; index++) header.createCell(columns[index]).setCellValue(names[index]);
            entry(sheet.createRow(14), LocalDate.of(2099, 8, 10), "Pagamento Boleto", "", -100, "Físico", "****5767");
            entry(sheet.createRow(15), LocalDate.of(2099, 8, 20), "Compra sintética", "Parcela 2 de 10", 100, "Virtual recorrente", "****5376");
            entry(sheet.createRow(16), LocalDate.of(2099, 8, 21), "Crédito sintético", "", -20, "Físico", "****5767");
            workbook.write(bytes);
            return parser.parse(new ByteArrayInputStream(bytes.toByteArray()));
        }
    }

    private void entry(Row row, LocalDate date, String description, String installment,
                       double amount, String cardType, String cardNumber) {
        row.createCell(1).setCellValue(DateUtil.getExcelDate(date.atStartOfDay()));
        row.createCell(2).setCellValue(description);
        if (!installment.isBlank()) row.createCell(3).setCellValue(installment);
        row.createCell(4).setCellValue(amount);
        row.createCell(6).setCellValue("Titular");
        row.createCell(7).setCellValue("Pessoa sintética");
        row.createCell(8).setCellValue(cardType);
        row.createCell(9).setCellValue(cardNumber);
    }
}
