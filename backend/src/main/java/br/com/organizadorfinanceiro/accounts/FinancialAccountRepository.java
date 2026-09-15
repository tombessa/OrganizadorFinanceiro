package br.com.organizadorfinanceiro.accounts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, UUID> {
    List<FinancialAccount> findAllByUserIdOrderByName(UUID userId);
    Optional<FinancialAccount> findByIdAndUserId(UUID id, UUID userId);
}

