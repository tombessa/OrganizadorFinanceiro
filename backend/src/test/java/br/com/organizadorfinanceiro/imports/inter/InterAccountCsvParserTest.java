package br.com.organizadorfinanceiro.imports.inter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class InterAccountCsvParserTest {
    private final InterAccountCsvParser parser = new InterAccountCsvParser();

    @Test
    void parsesMetadataRowsBrazilianValuesAndBalances() throws Exception {
        InterAccountStatement statement = parse("/inter/account-long.fixture");

        assertThat(statement.accountNumber()).isEqualTo("12345678");
        assertThat(statement.periodStart()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(statement.periodEnd()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(statement.endingBalance()).isEqualByComparingTo("160.00");
        assertThat(statement.entries()).hasSize(5);
        assertThat(statement.entries().get(1).signedAmount()).isEqualByComparingTo("-20.00");
        assertThat(statement.entries().get(4).balanceAfter()).isEqualByComparingTo("160.00");
        assertThat(statement.entries()).extracting(InterAccountStatement.Entry::sourceRowNumber)
                .containsExactly(7, 8, 9, 10, 11);
    }

    @Test
    void rejectsAStatementWhoseRunningBalanceDoesNotReconcile() {
        String invalid = """
                Extrato Conta Corrente
                Conta ;12345678
                Período ;01/01/2026 a 02/01/2026
                Saldo: ;90,00

                Data Lançamento;Descrição;Valor;Saldo
                01/01/2026;Crédito;100,00;100,00
                02/01/2026;Débito;-10,00;91,00
                """;

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("saldo não reconcilia na linha 8");
    }

    @Test
    void acceptsUtf8Bom() throws Exception {
        String content = "\ufeff" + new String(resource("/inter/account-short.fixture").readAllBytes(), StandardCharsets.UTF_8);
        InterAccountStatement statement = parser.parse(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        assertThat(statement.entries()).hasSize(2);
    }

    private InterAccountStatement parse(String path) throws Exception {
        try (InputStream input = resource(path)) {
            return parser.parse(input);
        }
    }

    private InputStream resource(String path) {
        InputStream input = getClass().getResourceAsStream(path);
        if (input == null) throw new IllegalStateException("Recurso ausente: " + path);
        return input;
    }
}
