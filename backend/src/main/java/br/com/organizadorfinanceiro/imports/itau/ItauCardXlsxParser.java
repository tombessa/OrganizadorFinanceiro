package br.com.organizadorfinanceiro.imports.itau;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.organizadorfinanceiro.imports.ImportDocumentStatus;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class ItauCardXlsxParser {
    private static final Pattern TITLE = Pattern.compile(
            "^Fatura\\s+(Paga|Aberta|Pr[oó]xima)\\s+-\\s+([\\p{L}çÇ]+)/(\\d{4})$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern INSTALLMENT = Pattern.compile(
            "^Parcela\\s+(\\d+)\\s+de\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LAST_FOUR = Pattern.compile("(\\d{4})\\s*$");
    private static final Map<String, Integer> MONTHS = months();

    public ItauCardStatement parse(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("A planilha Itaú não contém abas");
            }
            Sheet sheet = workbook.getSheetAt(0);
            TitleMetadata title = findTitle(sheet);
            int headerRowNumber = findHeaderRow(sheet);
            Map<String, Integer> columns = headerColumns(sheet.getRow(headerRowNumber));
            String masterCard = findValueBelowLabel(sheet, "CARTAO");
            String masterLastFour = lastFour(masterCard, "cartão principal");
            BigDecimal displayedTotal = numericValueBelowLabel(sheet, "VALOR");
            LocalDate dueDate = dateValueBelowLabel(sheet, "VENCIMENTO");

            List<ItauCardStatement.Entry> entries = new ArrayList<>();
            for (int rowNumber = headerRowNumber + 1; rowNumber <= sheet.getLastRowNum(); rowNumber++) {
                Row row = sheet.getRow(rowNumber);
                if (row == null) continue;
                Cell dateCell = row.getCell(columns.get("DATA"));
                if (dateCell == null || dateCell.getCellType() == CellType.BLANK) continue;
                if (dateCell.getCellType() != CellType.NUMERIC || !DateUtil.isValidExcelDate(dateCell.getNumericCellValue())) {
                    continue;
                }
                LocalDate date = DateUtil.getLocalDateTime(dateCell.getNumericCellValue()).toLocalDate();
                String description = requiredText(row.getCell(columns.get("LANCAMENTO")), "lançamento", rowNumber + 1);
                BigDecimal amount = requiredAmount(row.getCell(columns.get("VALOR")), rowNumber + 1);
                String installmentText = optionalText(row.getCell(columns.get("PARCELAMENTO")));
                Integer installmentNumber = null;
                Integer installmentTotal = null;
                if (!installmentText.isBlank()) {
                    Matcher installment = INSTALLMENT.matcher(installmentText);
                    if (!installment.matches()) {
                        throw new IllegalArgumentException("Parcelamento desconhecido na linha " + (rowNumber + 1));
                    }
                    installmentNumber = Integer.valueOf(installment.group(1));
                    installmentTotal = Integer.valueOf(installment.group(2));
                    if (installmentNumber < 1 || installmentNumber > installmentTotal) {
                        throw new IllegalArgumentException("Parcela inválida na linha " + (rowNumber + 1));
                    }
                }
                String usedCard = lastFour(requiredText(row.getCell(columns.get("NUMERO DO CARTAO")),
                        "número do cartão", rowNumber + 1), "cartão da linha " + (rowNumber + 1));
                String cardType = requiredText(row.getCell(columns.get("TIPO DO CARTAO")),
                        "tipo do cartão", rowNumber + 1);
                entries.add(new ItauCardStatement.Entry(rowNumber + 1, date, description, amount,
                        installmentNumber, installmentTotal, usedCard, cardType,
                        canonical(description).equals("PAGAMENTO BOLETO")));
            }

            BigDecimal reconciled = entries.stream()
                    .filter(entry -> !entry.statementPayment())
                    .map(ItauCardStatement.Entry::signedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            if (reconciled.compareTo(displayedTotal) != 0) {
                throw new IllegalArgumentException("O total da fatura Itaú não reconcilia: esperado "
                        + displayedTotal.toPlainString() + ", calculado " + reconciled.toPlainString());
            }
            return new ItauCardStatement(title.status(), title.competence(), dueDate,
                    displayedTotal, masterLastFour, entries);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Não foi possível ler a planilha Itaú", exception);
        }
    }

    private TitleMetadata findTitle(Sheet sheet) {
        for (int rowNumber = 0; rowNumber <= Math.min(sheet.getLastRowNum(), 30); rowNumber++) {
            Row row = sheet.getRow(rowNumber);
            if (row == null) continue;
            for (Cell cell : row) {
                String value = optionalText(cell);
                Matcher matcher = TITLE.matcher(value);
                if (!matcher.matches()) continue;
                ImportDocumentStatus status = switch (canonical(matcher.group(1))) {
                    case "PAGA" -> ImportDocumentStatus.PAID;
                    case "ABERTA" -> ImportDocumentStatus.OPEN;
                    case "PROXIMA" -> ImportDocumentStatus.PROJECTED;
                    default -> throw new IllegalArgumentException("Situação desconhecida na fatura Itaú");
                };
                Integer month = MONTHS.get(canonical(matcher.group(2)));
                if (month == null) throw new IllegalArgumentException("Mês desconhecido na fatura Itaú");
                return new TitleMetadata(status, YearMonth.of(Integer.parseInt(matcher.group(3)), month));
            }
        }
        throw new IllegalArgumentException("Título incompatível com a fatura Itaú");
    }

    private int findHeaderRow(Sheet sheet) {
        for (int rowNumber = 0; rowNumber <= Math.min(sheet.getLastRowNum(), 60); rowNumber++) {
            Row row = sheet.getRow(rowNumber);
            if (row == null) continue;
            Map<String, Integer> columns = headerColumns(row);
            if (columns.keySet().containsAll(List.of("DATA", "LANCAMENTO", "PARCELAMENTO", "VALOR",
                    "TIPO DO CARTAO", "NUMERO DO CARTAO"))) return rowNumber;
        }
        throw new IllegalArgumentException("Cabeçalho incompatível com a fatura Itaú");
    }

    private Map<String, Integer> headerColumns(Row row) {
        Map<String, Integer> columns = new HashMap<>();
        if (row == null) return columns;
        for (Cell cell : row) {
            String value = canonical(optionalText(cell));
            if (!value.isBlank()) columns.put(value, cell.getColumnIndex());
        }
        return columns;
    }

    private String findValueBelowLabel(Sheet sheet, String wanted) {
        Cell label = findLabel(sheet, wanted);
        Row next = sheet.getRow(label.getRowIndex() + 1);
        return requiredText(next == null ? null : next.getCell(label.getColumnIndex()), wanted.toLowerCase(Locale.ROOT),
                label.getRowIndex() + 2);
    }

    private BigDecimal numericValueBelowLabel(Sheet sheet, String wanted) {
        Cell label = findLabel(sheet, wanted);
        Row next = sheet.getRow(label.getRowIndex() + 1);
        return requiredAmount(next == null ? null : next.getCell(label.getColumnIndex()), label.getRowIndex() + 2);
    }

    private LocalDate dateValueBelowLabel(Sheet sheet, String wanted) {
        Cell label = findLabel(sheet, wanted);
        Row next = sheet.getRow(label.getRowIndex() + 1);
        Cell cell = next == null ? null : next.getCell(label.getColumnIndex());
        if (cell == null || cell.getCellType() != CellType.NUMERIC
                || !DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
            throw new IllegalArgumentException("Vencimento inválido na fatura Itaú");
        }
        return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
    }

    private Cell findLabel(Sheet sheet, String wanted) {
        for (int rowNumber = 0; rowNumber <= Math.min(sheet.getLastRowNum(), 30); rowNumber++) {
            Row row = sheet.getRow(rowNumber);
            if (row == null) continue;
            for (Cell cell : row) {
                String value = canonical(optionalText(cell));
                if (value.equals(wanted) || (wanted.equals("VALOR") && value.startsWith("VALOR "))) return cell;
            }
        }
        throw new IllegalArgumentException("Campo " + wanted.toLowerCase(Locale.ROOT) + " ausente na fatura Itaú");
    }

    private String requiredText(Cell cell, String field, int rowNumber) {
        String value = optionalText(cell);
        if (value.isBlank()) throw new IllegalArgumentException("Campo " + field + " vazio na linha " + rowNumber);
        return value;
    }

    private String optionalText(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        return "";
    }

    private BigDecimal requiredAmount(Cell cell, int rowNumber) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            throw new IllegalArgumentException("Valor inválido na linha " + rowNumber);
        }
        BigDecimal value = BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
        if (value.signum() == 0) throw new IllegalArgumentException("Valor zero na linha " + rowNumber);
        return value;
    }

    private String lastFour(String value, String field) {
        Matcher matcher = LAST_FOUR.matcher(value);
        if (!matcher.find()) throw new IllegalArgumentException("Não foi possível identificar os quatro últimos dígitos do " + field);
        return matcher.group(1);
    }

    static String canonical(String value) {
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static Map<String, Integer> months() {
        return Map.ofEntries(
                Map.entry("JANEIRO", 1), Map.entry("FEVEREIRO", 2), Map.entry("MARCO", 3),
                Map.entry("ABRIL", 4), Map.entry("MAIO", 5), Map.entry("JUNHO", 6),
                Map.entry("JULHO", 7), Map.entry("AGOSTO", 8), Map.entry("SETEMBRO", 9),
                Map.entry("OUTUBRO", 10), Map.entry("NOVEMBRO", 11), Map.entry("DEZEMBRO", 12));
    }

    private record TitleMetadata(ImportDocumentStatus status, YearMonth competence) {}
}
