package br.com.organizadorfinanceiro.imports.inter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class InterAccountCsvParser {
    private static final String TITLE = "Extrato Conta Corrente";
    private static final String HEADER = "Data Lançamento;Descrição;Valor;Saldo";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern PERIOD = Pattern.compile("^(\\d{2}/\\d{2}/\\d{4})\\s+a\\s+(\\d{2}/\\d{2}/\\d{4})$");

    public InterAccountStatement parse(InputStream input) throws IOException {
        List<String> lines = readLines(input);
        if (lines.isEmpty()) throw invalid("arquivo vazio");
        lines.set(0, stripBom(lines.get(0)));
        if (!TITLE.equals(lines.get(0).trim())) throw invalid("título do extrato não reconhecido");

        int headerIndex = lines.indexOf(HEADER);
        if (headerIndex < 0) throw invalid("cabeçalho esperado não encontrado");

        String accountNumber = metadata(lines, headerIndex, "Conta");
        String rawPeriod = metadata(lines, headerIndex, "Período");
        BigDecimal endingBalance = parseMoney(metadata(lines, headerIndex, "Saldo:"), "saldo final");
        Matcher period = PERIOD.matcher(rawPeriod);
        if (!period.matches()) throw invalid("período inválido");

        List<InterAccountStatement.Entry> entries = new ArrayList<>();
        for (int index = headerIndex + 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) continue;
            String[] fields = line.split(";", -1);
            if (fields.length != 4) throw invalid("linha " + (index + 1) + " deve possuir quatro campos");
            String rawDate = fields[0].trim();
            String description = normalizeDescription(fields[1]);
            String rawAmount = fields[2].trim();
            String rawBalance = fields[3].trim();
            if (description.isBlank()) throw invalid("descrição ausente na linha " + (index + 1));
            BigDecimal amount = parseMoney(rawAmount, "valor da linha " + (index + 1));
            if (amount.signum() == 0) throw invalid("valor zero não suportado na linha " + (index + 1));
            entries.add(new InterAccountStatement.Entry(
                    index + 1, rawDate, parseDate(rawDate, index + 1), description,
                    rawAmount, amount, rawBalance, parseMoney(rawBalance, "saldo da linha " + (index + 1))));
        }
        if (entries.isEmpty()) throw invalid("nenhum lançamento encontrado");
        validateBalances(entries, endingBalance);
        return new InterAccountStatement(accountNumber, parseDate(period.group(1), 3),
                parseDate(period.group(2), 3), endingBalance, entries);
    }

    private List<String> readLines(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return new ArrayList<>(reader.lines().toList());
        }
    }

    private String metadata(List<String> lines, int headerIndex, String label) {
        for (int index = 1; index < headerIndex; index++) {
            String[] fields = lines.get(index).split(";", 2);
            if (fields.length == 2 && label.equals(fields[0].trim())) {
                String value = fields[1].trim();
                if (!value.isBlank()) return value;
            }
        }
        throw invalid("metadado " + label + " não encontrado");
    }

    private void validateBalances(List<InterAccountStatement.Entry> entries, BigDecimal endingBalance) {
        for (int index = 1; index < entries.size(); index++) {
            InterAccountStatement.Entry previous = entries.get(index - 1);
            InterAccountStatement.Entry current = entries.get(index);
            BigDecimal expected = previous.balanceAfter().add(current.signedAmount());
            if (expected.compareTo(current.balanceAfter()) != 0) {
                throw invalid("saldo não reconcilia na linha " + current.sourceRowNumber());
            }
        }
        InterAccountStatement.Entry last = entries.get(entries.size() - 1);
        if (last.balanceAfter().compareTo(endingBalance) != 0) {
            throw invalid("saldo final difere do último lançamento");
        }
    }

    static String normalizeDescription(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
                .trim().replaceAll("\\s+", " ");
    }

    private BigDecimal parseMoney(String value, String field) {
        try {
            String normalized = value.trim().replace(".", "").replace(',', '.');
            if (!normalized.matches("-?\\d+(\\.\\d{1,2})?")) throw new NumberFormatException();
            return new BigDecimal(normalized).setScale(2);
        } catch (NumberFormatException exception) {
            throw invalid(field + " inválido");
        }
    }

    private LocalDate parseDate(String value, int line) {
        try {
            return LocalDate.parse(value, DATE);
        } catch (DateTimeParseException exception) {
            throw invalid("data inválida na linha " + line);
        }
    }

    private String stripBom(String value) {
        return value.startsWith("\ufeff") ? value.substring(1) : value;
    }

    private IllegalArgumentException invalid(String detail) {
        return new IllegalArgumentException("Extrato Inter inválido: " + detail);
    }
}
