package br.com.organizadorfinanceiro.imports;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportFileRepository extends JpaRepository<ImportFile, UUID> {
    Optional<ImportFile> findByUserIdAndSourceAdapterAndContentHash(
            UUID userId, SourceAdapter sourceAdapter, String contentHash);
}

