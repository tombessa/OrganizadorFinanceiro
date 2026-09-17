package br.com.organizadorfinanceiro.imports.inter;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import br.com.organizadorfinanceiro.imports.SourceAdapter;
import br.com.organizadorfinanceiro.shared.HashingService;
import org.springframework.stereotype.Component;

@Component
public class InterAccountFingerprint {
    private final HashingService hashingService;

    public InterAccountFingerprint(HashingService hashingService) {
        this.hashingService = hashingService;
    }

    public String create(UUID accountId, InterAccountStatement.Entry entry) {
        String canonical = String.join("|",
                SourceAdapter.INTER_ACCOUNT_CSV.name(),
                accountId.toString(),
                entry.date().toString(),
                entry.description().toUpperCase(Locale.ROOT),
                decimal(entry.signedAmount()),
                decimal(entry.balanceAfter()));
        return hashingService.sha256(canonical);
    }

    private String decimal(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
