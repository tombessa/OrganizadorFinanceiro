package br.com.organizadorfinanceiro.imports.itau;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.organizadorfinanceiro.shared.HashingService;
import org.springframework.stereotype.Component;

@Component
public class ItauCardFingerprint {
    private final HashingService hashingService;

    public ItauCardFingerprint(HashingService hashingService) {
        this.hashingService = hashingService;
    }

    public String create(UUID cardId, ItauCardStatement statement,
                         ItauCardStatement.Entry entry, int occurrence) {
        return hashingService.sha256(String.join("|",
                "ITAU_CARD_XLSX",
                cardId.toString(),
                statement.competence().toString(),
                entry.date().toString(),
                ItauCardXlsxParser.canonical(entry.description()),
                amount(entry.signedAmount()),
                entry.installmentNumber() == null ? "" : entry.installmentNumber().toString(),
                entry.installmentTotal() == null ? "" : entry.installmentTotal().toString(),
                entry.usedCardLastFourDigits(),
                Boolean.toString(entry.statementPayment()),
                Integer.toString(occurrence)));
    }

    String businessKey(ItauCardStatement.Entry entry) {
        return String.join("|", entry.date().toString(),
                ItauCardXlsxParser.canonical(entry.description()), amount(entry.signedAmount()),
                entry.installmentNumber() == null ? "" : entry.installmentNumber().toString(),
                entry.installmentTotal() == null ? "" : entry.installmentTotal().toString(),
                entry.usedCardLastFourDigits());
    }

    private String amount(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
