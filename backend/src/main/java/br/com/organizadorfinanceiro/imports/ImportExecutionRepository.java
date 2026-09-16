package br.com.organizadorfinanceiro.imports;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportExecutionRepository extends JpaRepository<ImportExecution, UUID> {
    Optional<ImportExecution> findFirstByUserIdAndImportFileIdOrderByStartedAtDesc(UUID userId, UUID importFileId);
}
