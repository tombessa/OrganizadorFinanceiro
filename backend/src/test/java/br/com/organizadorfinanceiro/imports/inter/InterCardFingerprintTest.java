package br.com.organizadorfinanceiro.imports.inter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.UUID;

import br.com.organizadorfinanceiro.shared.HashingService;
import org.junit.jupiter.api.Test;

class InterCardFingerprintTest {
    private final InterCardFingerprint fingerprint = new InterCardFingerprint(new HashingService());

    @Test
    void separatesInstallmentPositionsCardsAndLegitimateRepeatedRows() throws Exception {
        UUID firstCard = UUID.fromString("cf4b6372-9df5-4ea5-a9d8-27c7869498d8");
        UUID secondCard = UUID.fromString("d05869da-8dfa-4ca5-a40c-582468d53e03");
        InterCardStatement.Entry entry = parse("/inter/card-projected.fixture").entries().getFirst();

        assertThat(fingerprint.create(firstCard, entry, 1))
                .isNotEqualTo(fingerprint.create(secondCard, entry, 1));
        assertThat(fingerprint.create(firstCard, entry, 1))
                .isNotEqualTo(fingerprint.create(firstCard, entry, 2));
    }

    private InterCardStatement parse(String path) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Recurso ausente: " + path);
            return new InterCardCsvParser().parse(input);
        }
    }
}
