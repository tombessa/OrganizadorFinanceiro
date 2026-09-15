package br.com.organizadorfinanceiro.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class HashingServiceTest {
    @Test
    void shouldCalculateStableSha256() throws Exception {
        String hash = new HashingService().sha256(new ByteArrayInputStream("organizador-financeiro"
                .getBytes(StandardCharsets.UTF_8)));

        assertThat(hash).isEqualTo("f6453b4b01d6592a7a34cda56e805da3ce430c40035cb06c391a039a0e6fba7c");
    }
}
