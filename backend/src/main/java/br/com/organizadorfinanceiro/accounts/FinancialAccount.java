package br.com.organizadorfinanceiro.accounts;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.organizadorfinanceiro.institutions.FinancialInstitution;
import br.com.organizadorfinanceiro.shared.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "financial_account", schema = "organizadorfinanceiro")
public class FinancialAccount extends OwnedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private FinancialInstitution institution;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Column(nullable = false, length = 3)
    private String currency = "BRL";

    @Column(name = "overdraft_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    protected FinancialAccount() {
    }

    public FinancialAccount(UUID userId, FinancialInstitution institution, String name,
                            AccountType accountType, BigDecimal overdraftLimit) {
        super(userId);
        this.institution = institution;
        this.name = name.trim();
        this.accountType = accountType;
        this.overdraftLimit = overdraftLimit == null ? BigDecimal.ZERO : overdraftLimit;
    }

    public FinancialInstitution getInstitution() { return institution; }
    public String getName() { return name; }
    public AccountType getAccountType() { return accountType; }
    public String getCurrency() { return currency; }
    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
    public boolean isActive() { return active; }
}

