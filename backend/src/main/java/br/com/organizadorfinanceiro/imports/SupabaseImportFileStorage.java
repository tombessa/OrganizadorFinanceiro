package br.com.organizadorfinanceiro.imports;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@ConditionalOnProperty(name = "app.import-storage.mode", havingValue = "supabase", matchIfMissing = true)
public class SupabaseImportFileStorage implements ImportFileStorage {
    private final HttpClient client;
    private final String baseUrl;
    private final String bucket;

    @Autowired
    public SupabaseImportFileStorage(ImportStorageProperties properties) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), properties);
    }

    SupabaseImportFileStorage(HttpClient client, ImportStorageProperties properties) {
        this.client = client;
        this.baseUrl = normalizeBaseUrl(properties.getSupabaseUrl());
        this.bucket = properties.getBucket();
    }

    @Override
    public StoredObject store(UUID userId, SourceAdapter adapter, String contentHash,
                              MultipartFile file, Credentials credentials) throws IOException {
        validateCredentials(credentials);
        String objectPath = StoragePathFactory.create(userId, adapter, contentHash, file.getOriginalFilename());
        HttpRequest request = request(objectPath, credentials)
                .header("Content-Type", contentType(adapter))
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> inputStream(file)))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Falha ao armazenar o arquivo (Storage HTTP " + response.statusCode() + ")");
        }
        return new StoredObject(bucket, objectPath);
    }

    @Override
    public void delete(StoredObject object, Credentials credentials) {
        if (credentials == null || credentials.accessToken() == null || credentials.apiKey() == null) return;
        try {
            send(request(object.path(), credentials).DELETE().build());
        } catch (IOException ignored) {
            // Cleanup is best effort. The database transaction still rolls back.
        }
    }

    private HttpRequest.Builder request(String objectPath, Credentials credentials) {
        return HttpRequest.newBuilder(URI.create(baseUrl + "/storage/v1/object/" + bucket + "/" + objectPath))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + credentials.accessToken())
                .header("apikey", credentials.apiKey());
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Armazenamento interrompido", exception);
        }
    }

    private static java.io.InputStream inputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String contentType(SourceAdapter adapter) {
        return switch (adapter) {
            case INTER_ACCOUNT_CSV, INTER_CARD_CSV -> "text/csv";
            case ITAU_CARD_XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case SANTANDER_ACCOUNT_PDF, PAYROLL_PDF -> "application/pdf";
        };
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("SUPABASE_URL não configurada");
        String normalized = value.trim().replaceAll("/+$", "");
        if (normalized.endsWith("/auth/v1")) normalized = normalized.substring(0, normalized.length() - 8);
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            throw new IllegalStateException("URL do Supabase inválida");
        }
        return normalized;
    }

    private static void validateCredentials(Credentials credentials) {
        if (credentials == null || credentials.accessToken() == null || credentials.accessToken().isBlank()
                || credentials.apiKey() == null || credentials.apiKey().isBlank()) {
            throw new IllegalArgumentException("Credenciais de armazenamento ausentes");
        }
    }
}
