package br.com.organizadorfinanceiro.transactions;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {
    boolean existsByUserIdAndTransactionFingerprint(UUID userId, String transactionFingerprint);
    Optional<FinancialTransaction> findByUserIdAndTransactionFingerprint(UUID userId, String transactionFingerprint);
}
