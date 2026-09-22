package br.com.organizadorfinanceiro.imports.itau;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import br.com.organizadorfinanceiro.imports.ImportDocumentStatus;

public record ItauCardStatement(ImportDocumentStatus documentStatus, YearMonth competence,
                                LocalDate dueDate, BigDecimal displayedTotal,
                                String masterCardLastFourDigits, List<Entry> entries) {
    public ItauCardStatement {
        entries = List.copyOf(entries);
        if (entries.isEmpty()) throw new IllegalArgumentException("A fatura Itaú não contém lançamentos");
    }

    public record Entry(int sourceRowNumber, LocalDate date, String description,
                        BigDecimal signedAmount, Integer installmentNumber,
                        Integer installmentTotal, String usedCardLastFourDigits,
                        String cardType, boolean statementPayment) {
        public boolean installment() {
            return installmentNumber != null;
        }
    }
}
