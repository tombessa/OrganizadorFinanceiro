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
public class InterCardCsvParser {
    private static final List<String> HEADER = List.of("Data", "Lançamento", "Categoria", "Tipo", "Valor");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern INSTALLMENT = Pattern.compile("^Parcela\\s+(\\d+)/(\\d+)$", Pattern.CASE_INSENSITIVE);

    public InterCardStatement parse(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("A fatura Inter está vazia");
            if (headerLine.startsWith("\ufeff")) headerLine = headerLine.substring(1);
            if (!parseCsvLine(headerLine, 1).equals(HEADER)) {
                throw new IllegalArgumentException("Cabeçalho incompatível com a fatura de cartão Inter");
            }

            List<InterCardStatement.Entry> entries = new ArrayList<>();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) continue;
                List<String> fields = parseCsvLine(line, rowNumber);
                if (fields.size() != HEADER.size()) {
                    throw new IllegalArgumentException("Quantidade de colunas inválida na linha " + rowNumber);
                }
                String type = required(fields.get(3), "tipo", rowNumber);
                Integer installmentNumber = null;
                Integer installmentTotal = null;
                Matcher installment = INSTALLMENT.matcher(type);
                if (installment.matches()) {
                    installmentNumber = Integer.valueOf(installment.group(1));
                    installmentTotal = Integer.valueOf(installment.group(2));
                    if (installmentNumber < 1 || installmentNumber > installmentTotal) {
                        throw new IllegalArgumentException("Parcela inválida na linha " + rowNumber);
                    }
                } else if (!type.equalsIgnoreCase("Compra à vista")) {
                    throw new IllegalArgumentException("Tipo de lançamento Inter desconhecido na linha " + rowNumber + ": " + type);
                }
                String rawDate = required(fields.get(0), "data", rowNumber);
                String rawAmount = required(fields.get(4), "valor", rowNumber);
                entries.add(new InterCardStatement.Entry(
                        rowNumber,
                        rawDate,
                        parseDate(rawDate, rowNumber),
                        required(fields.get(1), "lançamento", rowNumber),
                        required(fields.get(2), "categoria", rowNumber),
                        type,
                        rawAmount,
                        parseAmount(rawAmount, rowNumber),
                        installmentNumber,
                        installmentTotal));
            }
            return new InterCardStatement(entries);
        }
    }

    private List<String> parseCsvLine(String line, int rowNumber) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (quoted) throw new IllegalArgumentException("Aspas não finalizadas na linha " + rowNumber);
        fields.add(current.toString());
        return fields;
    }

    private String required(String value, String field, int rowNumber) {
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Campo " + field + " vazio na linha " + rowNumber);
        return normalized;
    }

    private LocalDate parseDate(String value, int rowNumber) {
        try {
            return LocalDate.parse(value, DATE);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Data inválida na linha " + rowNumber + ": " + value, exception);
        }
    }

    private BigDecimal parseAmount(String value, int rowNumber) {
        String normalized = value.replace("\u00a0", "").replace(" ", "").replace("R$", "")
                .replace(".", "").replace(',', '.');
        try {
            BigDecimal amount = new BigDecimal(normalized);
            if (amount.signum() == 0) throw new IllegalArgumentException("Valor zero na linha " + rowNumber);
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Valor inválido na linha " + rowNumber + ": " + value, exception);
        }
    }
}
