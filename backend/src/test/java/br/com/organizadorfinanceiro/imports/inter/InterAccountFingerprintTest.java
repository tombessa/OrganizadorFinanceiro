package br.com.organizadorfinanceiro.imports.inter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.UUID;

import br.com.organizadorfinanceiro.shared.HashingService;
import org.junit.jupiter.api.Test;

class InterAccountFingerprintTest {
    private final InterAccountCsvParser parser = new InterAccountCsvParser();
    private final InterAccountFingerprint fingerprint = new InterAccountFingerprint(new HashingService());

    @Test
    void identifiesOverlapAcrossDifferentStatementFilesWithoutCrossingAccounts() throws Exception {
        InterAccountStatement longStatement = parse("/inter/account-long.fixture");
        InterAccountStatement shortStatement = parse("/inter/account-short.fixture");
        UUID account = UUID.fromString("d415325d-fd7f-4cc5-9164-1f63d398501e");
        UUID otherAccount = UUID.fromString("87c3bbb9-2473-47e9-a56f-358abeb8f9c5");

        String fromLong = fingerprint.create(account, longStatement.entries().get(3));
        String fromShort = fingerprint.create(account, shortStatement.entries().get(0));

        assertThat(fromLong).isEqualTo(fromShort).hasSize(64);
        assertThat(fingerprint.create(otherAccount, shortStatement.entries().get(0))).isNotEqualTo(fromLong);
        assertThat(fingerprint.create(account, longStatement.entries().get(4))).isNotEqualTo(fromLong);
    }

    private InterAccountStatement parse(String path) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Recurso ausente: " + path);
            return parser.parse(input);
        }
    }
}
