package br.com.organizadorfinanceiro.imports;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FilesystemImportFileStorageTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAndDeletesPrivateImport() throws Exception {
        ImportStorageProperties properties = new ImportStorageProperties();
        properties.setRoot(temporaryDirectory.toString());
        FilesystemImportFileStorage storage = new FilesystemImportFileStorage(properties);
        UUID userId = UUID.randomUUID();
        String hash = "b".repeat(64);
        MockMultipartFile file = new MockMultipartFile(
                "file", "extrato.csv", "text/csv", "data;valor".getBytes(StandardCharsets.UTF_8));

        ImportFileStorage.StoredObject stored = storage.store(
                userId, SourceAdapter.INTER_ACCOUNT_CSV, hash, file,
                new ImportFileStorage.Credentials(null, null));

        Path storedPath = temporaryDirectory.resolve(stored.path());
        assertThat(stored.bucket()).isEqualTo(FilesystemImportFileStorage.BUCKET);
        assertThat(storedPath).hasContent("data;valor");

        storage.delete(stored, new ImportFileStorage.Credentials(null, null));
        assertThat(storedPath).doesNotExist();
    }
}
