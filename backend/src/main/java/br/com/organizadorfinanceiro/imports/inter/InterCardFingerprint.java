package br.com.organizadorfinanceiro.imports.inter;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

import br.com.organizadorfinanceiro.shared.HashingService;
import org.springframework.stereotype.Component;

@Component
public class InterCardFingerprint {
    private final HashingService hashingService;

    public InterCardFingerprint(HashingService hashingService) {
        this.hashingService = hashingService;
    }

    public String create(UUID cardId, InterCardStatement.Entry entry, int occurrence) {
        String material = String.join("|",
                "INTER_CARD_CSV",
                cardId.toString(),
                entry.date().toString(),
                canonical(entry.description()),
                canonical(entry.category()),
                canonical(entry.type()),
                amount(entry.signedAmount()),
                entry.installmentNumber() == null ? "" : entry.installmentNumber().toString(),
                entry.installmentTotal() == null ? "" : entry.installmentTotal().toString(),
                Integer.toString(occurrence));
        return hashingService.sha256(material);
    }

    String businessKey(InterCardStatement.Entry entry) {
        return String.join("|", entry.date().toString(), canonical(entry.description()),
                canonical(entry.category()), canonical(entry.type()), amount(entry.signedAmount()));
    }

    private String canonical(String value) {
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private String amount(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
