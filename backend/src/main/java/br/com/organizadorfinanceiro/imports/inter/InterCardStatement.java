package br.com.organizadorfinanceiro.imports.inter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InterCardStatement(List<Entry> entries) {
    public InterCardStatement {
        entries = List.copyOf(entries);
        if (entries.isEmpty()) throw new IllegalArgumentException("A fatura Inter não contém lançamentos");
    }

    public record Entry(int sourceRowNumber, String rawDate, LocalDate date, String description,
                        String category, String type, String rawAmount, BigDecimal signedAmount,
                        Integer installmentNumber, Integer installmentTotal) {
        public boolean installment() {
            return installmentNumber != null;
        }
    }
}
