package br.com.organizadorfinanceiro.imports.inter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class InterCardCsvParserTest {
    private final InterCardCsvParser parser = new InterCardCsvParser();

    @Test
    void parsesQuotedFieldsBrazilianAmountsCreditsAndInstallments() throws Exception {
        InterCardStatement posted = parse("/inter/card-posted.fixture");
        InterCardStatement projected = parse("/inter/card-projected.fixture");

        assertThat(posted.entries()).hasSize(3);
        assertThat(posted.entries().get(0).date()).isEqualTo(LocalDate.of(2099, 7, 31));
        assertThat(posted.entries().get(0).description()).isEqualTo("CENARIO SINTETICO, ALFA");
        assertThat(posted.entries().get(0).signedAmount()).isEqualByComparingTo("123.45");
        assertThat(posted.entries().get(2).signedAmount()).isEqualByComparingTo("-135.79");
        assertThat(projected.entries().getFirst().installmentNumber()).isEqualTo(2);
        assertThat(projected.entries().getFirst().installmentTotal()).isEqualTo(10);
    }

    @Test
    void acceptsUtf8Bom() throws Exception {
        String content = "\ufeff" + new String(resource("/inter/card-projected.fixture").readAllBytes(), StandardCharsets.UTF_8);
        InterCardStatement statement = parser.parse(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        assertThat(statement.entries()).hasSize(1);
    }

    @Test
    void rejectsInvalidInstallmentPosition() {
        String invalid = """
                "Data","Lançamento","Categoria","Tipo","Valor"
                "09/09/2099","CENARIO SINTETICO INVALIDO","OUTROS","Parcela 11/10","R$ 10,00"
                """;
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parcela inválida na linha 2");
    }

    @Test
    void rejectsUnknownLayout() {
        String invalid = "Data,Descrição,Valor\n01/01/2026,Compra,10,00";
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cabeçalho incompatível");
    }

    private InterCardStatement parse(String path) throws Exception {
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
