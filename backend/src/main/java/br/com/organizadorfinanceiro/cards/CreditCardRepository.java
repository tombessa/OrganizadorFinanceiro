package br.com.organizadorfinanceiro.cards;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {
    List<CreditCard> findAllByUserIdOrderByName(UUID userId);
}

