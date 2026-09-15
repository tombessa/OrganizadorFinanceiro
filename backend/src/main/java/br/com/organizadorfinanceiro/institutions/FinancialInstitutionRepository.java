package br.com.organizadorfinanceiro.institutions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialInstitutionRepository extends JpaRepository<FinancialInstitution, UUID> {
    List<FinancialInstitution> findAllByUserIdOrderByName(UUID userId);
    Optional<FinancialInstitution> findByIdAndUserId(UUID id, UUID userId);
}

