package br.com.organizadorfinanceiro.imports;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportWarningRepository extends JpaRepository<ImportWarning, UUID> {
}
