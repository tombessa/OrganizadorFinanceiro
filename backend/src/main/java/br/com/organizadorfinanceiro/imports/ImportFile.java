package br.com.organizadorfinanceiro.imports;

import java.time.Instant;
import java.util.UUID;

import br.com.organizadorfinanceiro.shared.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "import_file", schema = "organizadorfinanceiro")
public class ImportFile extends OwnedEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "source_adapter", nullable = false, length = 60)
    private SourceAdapter sourceAdapter;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "storage_bucket", nullable = false, length = 100)
    private String storageBucket;

    @Column(name = "storage_path", nullable = false, length = 700)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_status", nullable = false, length = 20)
    private StorageStatus storageStatus;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected ImportFile() {
    }

    public ImportFile(UUID userId, SourceAdapter sourceAdapter, String originalFilename,
                      String contentType, long byteSize, String contentHash,
                      String storageBucket, String storagePath) {
        super(userId);
        this.sourceAdapter = sourceAdapter;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.byteSize = byteSize;
        this.contentHash = contentHash;
        this.storageBucket = storageBucket;
        this.storagePath = storagePath;
        this.storageStatus = StorageStatus.STORED;
        this.receivedAt = Instant.now();
    }

    public SourceAdapter getSourceAdapter() { return sourceAdapter; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getByteSize() { return byteSize; }
    public String getContentHash() { return contentHash; }
    public String getStorageBucket() { return storageBucket; }
    public String getStoragePath() { return storagePath; }
    public StorageStatus getStorageStatus() { return storageStatus; }
    public Instant getReceivedAt() { return receivedAt; }
}
