package br.com.organizadorfinanceiro.imports;

import java.util.Locale;
import java.util.UUID;

final class StoragePathFactory {
    private StoragePathFactory() {}

    static String create(UUID userId, SourceAdapter adapter, String contentHash, String filename) {
        String extension = extension(filename);
        return userId + "/" + adapter.name().toLowerCase(Locale.ROOT) + "/" + contentHash + extension;
    }

    private static String extension(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) return ".csv";
        if (lower.endsWith(".xlsx")) return ".xlsx";
        if (lower.endsWith(".pdf")) return ".pdf";
        throw new IllegalArgumentException("Extensão de arquivo não suportada");
    }
}
