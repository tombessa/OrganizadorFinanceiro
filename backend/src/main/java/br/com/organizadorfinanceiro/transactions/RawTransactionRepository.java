package br.com.organizadorfinanceiro.transactions;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RawTransactionRepository extends JpaRepository<RawTransaction, UUID> {
    boolean existsByUserIdAndRawFingerprint(UUID userId, String rawFingerprint);

    @Query("select raw.rawFingerprint from RawTransaction raw "
            + "where raw.userId = :userId and raw.rawFingerprint in :fingerprints")
    Set<String> findExistingFingerprints(@Param("userId") UUID userId,
                                         @Param("fingerprints") Collection<String> fingerprints);
}
