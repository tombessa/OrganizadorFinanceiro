package br.com.organizadorfinanceiro.imports;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@ConditionalOnProperty(name = "app.import-storage.mode", havingValue = "filesystem")
public class FilesystemImportFileStorage implements ImportFileStorage {
    static final String BUCKET = "local-private-imports";
    private final Path root;

    public FilesystemImportFileStorage(ImportStorageProperties properties) {
        this.root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
    }

    @Override
    public StoredObject store(UUID userId, SourceAdapter adapter, String contentHash,
                              MultipartFile file, Credentials credentials) throws IOException {
        String objectPath = StoragePathFactory.create(userId, adapter, contentHash, file.getOriginalFilename());
        Path destination = safeResolve(objectPath);
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".upload-", ".tmp");
        try {
            file.transferTo(temporary);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination);
            }
        } catch (java.nio.file.FileAlreadyExistsException ignored) {
            Files.deleteIfExists(temporary);
        } catch (IOException exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }
        return new StoredObject(BUCKET, objectPath);
    }

    @Override
    public void delete(StoredObject object, Credentials credentials) {
        try {
            Files.deleteIfExists(safeResolve(object.path()));
        } catch (IOException ignored) {
            // Cleanup is best effort. The database transaction still rolls back.
        }
    }

    private Path safeResolve(String objectPath) {
        Path resolved = root.resolve(objectPath).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Caminho de armazenamento inválido");
        return resolved;
    }
}
