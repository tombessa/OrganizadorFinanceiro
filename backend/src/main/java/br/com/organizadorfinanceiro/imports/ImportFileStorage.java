package br.com.organizadorfinanceiro.imports;

import java.io.IOException;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

public interface ImportFileStorage {
    StoredObject store(UUID userId, SourceAdapter adapter, String contentHash,
                       MultipartFile file, Credentials credentials) throws IOException;

    void delete(StoredObject object, Credentials credentials);

    record StoredObject(String bucket, String path) {}
    record Credentials(String accessToken, String apiKey) {}
}
