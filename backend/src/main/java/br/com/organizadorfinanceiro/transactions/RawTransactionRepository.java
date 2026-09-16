package br.com.organizadorfinanceiro.transactions;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RawTransactionRepository extends JpaRepository<RawTransaction, UUID> {
    boolean existsByUserIdAndRawFingerprint(UUID userId, String rawFingerprint);
}
