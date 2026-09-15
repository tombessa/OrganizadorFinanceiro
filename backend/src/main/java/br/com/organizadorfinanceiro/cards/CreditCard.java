package br.com.organizadorfinanceiro.cards;

import java.util.UUID;

import br.com.organizadorfinanceiro.accounts.FinancialAccount;
import br.com.organizadorfinanceiro.institutions.FinancialInstitution;
import br.com.organizadorfinanceiro.shared.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_card", schema = "organizadorfinanceiro")
public class CreditCard extends OwnedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private FinancialInstitution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id")
    private FinancialAccount paymentAccount;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @Column(name = "closing_day", nullable = false)
    private short closingDay;

    @Column(name = "due_day", nullable = false)
    private short dueDay;

    @Column(nullable = false)
    private boolean active = true;

    protected CreditCard() {
    }

    public CreditCard(UUID userId, FinancialInstitution institution, FinancialAccount paymentAccount,
                      String name, String lastFourDigits, short closingDay, short dueDay) {
        super(userId);
        this.institution = institution;
        this.paymentAccount = paymentAccount;
        this.name = name.trim();
        this.lastFourDigits = lastFourDigits;
        this.closingDay = closingDay;
        this.dueDay = dueDay;
    }

    public FinancialInstitution getInstitution() { return institution; }
    public FinancialAccount getPaymentAccount() { return paymentAccount; }
    public String getName() { return name; }
    public String getLastFourDigits() { return lastFourDigits; }
    public short getClosingDay() { return closingDay; }
    public short getDueDay() { return dueDay; }
    public boolean isActive() { return active; }
}

