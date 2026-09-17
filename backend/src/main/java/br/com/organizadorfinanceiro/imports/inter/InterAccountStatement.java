package br.com.organizadorfinanceiro.imports.inter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InterAccountStatement(
        String accountNumber,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal endingBalance,
        List<Entry> entries) {

    public InterAccountStatement {
        entries = List.copyOf(entries);
    }

    public record Entry(
            int sourceRowNumber,
            String rawDate,
            LocalDate date,
            String description,
            String rawAmount,
            BigDecimal signedAmount,
            String rawBalance,
            BigDecimal balanceAfter) {
    }
}
