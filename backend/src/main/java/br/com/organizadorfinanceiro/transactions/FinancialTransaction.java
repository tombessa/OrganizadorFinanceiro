package br.com.organizadorfinanceiro.transactions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.com.organizadorfinanceiro.accounts.FinancialAccount;
import br.com.organizadorfinanceiro.cards.CreditCard;
import br.com.organizadorfinanceiro.imports.SourceAdapter;
import br.com.organizadorfinanceiro.shared.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "financial_transaction", schema = "organizadorfinanceiro")
public class FinancialTransaction extends OwnedEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_transaction_id", nullable = false)
    private RawTransaction rawTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private FinancialAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "posting_status", nullable = false, length = 20)
    private TransactionPostingStatus postingStatus;

    @Column(name = "raw_description", nullable = false)
    private String rawDescription;

    @Column(name = "normalized_description")
    private String normalizedDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private SourceAdapter source;

    @Column(name = "source_reference", length = 255)
    private String sourceReference;

    @Column(name = "transaction_fingerprint", nullable = false, length = 64)
    private String transactionFingerprint;

    @Column(name = "classification_confidence", precision = 5, scale = 4)
    private BigDecimal classificationConfidence;

    @Column(name = "is_transfer", nullable = false)
    private boolean transfer;

    @Column(name = "is_reimbursement", nullable = false)
    private boolean reimbursement;

    @Column(name = "is_statement_payment", nullable = false)
    private boolean statementPayment;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;

    protected FinancialTransaction() {
    }

    private FinancialTransaction(UUID userId) {
        super(userId);
    }

    public static FinancialTransaction fromAccountStatement(UUID userId, RawTransaction rawTransaction,
                                                             FinancialAccount account, LocalDate transactionDate,
                                                             BigDecimal signedAmount, String description,
                                                             String sourceReference, String fingerprint) {
        FinancialTransaction transaction = new FinancialTransaction(userId);
        transaction.rawTransaction = rawTransaction;
        transaction.account = account;
        transaction.transactionDate = transactionDate;
        transaction.postingDate = transactionDate;
        transaction.amount = signedAmount.abs();
        transaction.currency = "BRL";
        transaction.direction = signedAmount.signum() < 0
                ? TransactionDirection.DEBIT : TransactionDirection.CREDIT;
        transaction.transactionType = TransactionType.ADJUSTMENT;
        transaction.postingStatus = TransactionPostingStatus.POSTED;
        transaction.rawDescription = description;
        transaction.normalizedDescription = description;
        transaction.source = SourceAdapter.INTER_ACCOUNT_CSV;
        transaction.sourceReference = sourceReference;
        transaction.transactionFingerprint = fingerprint;
        return transaction;
    }

    public static FinancialTransaction fromCardStatement(UUID userId, RawTransaction rawTransaction,
                                                          CreditCard creditCard, LocalDate transactionDate,
                                                          BigDecimal signedAmount, String description,
                                                          String sourceReference, String fingerprint,
                                                          TransactionPostingStatus postingStatus) {
        FinancialTransaction transaction = new FinancialTransaction(userId);
        transaction.rawTransaction = rawTransaction;
        transaction.creditCard = creditCard;
        transaction.transactionDate = transactionDate;
        transaction.postingDate = postingStatus == TransactionPostingStatus.POSTED ? transactionDate : null;
        transaction.amount = signedAmount.abs();
        transaction.currency = "BRL";
        transaction.direction = signedAmount.signum() < 0
                ? TransactionDirection.CREDIT : TransactionDirection.DEBIT;
        transaction.transactionType = TransactionType.ADJUSTMENT;
        transaction.postingStatus = postingStatus;
        transaction.rawDescription = description;
        transaction.normalizedDescription = description;
        transaction.source = SourceAdapter.INTER_CARD_CSV;
        transaction.sourceReference = sourceReference;
        transaction.transactionFingerprint = fingerprint;
        return transaction;
    }

    public RawTransaction getRawTransaction() { return rawTransaction; }
    public FinancialAccount getAccount() { return account; }
    public CreditCard getCreditCard() { return creditCard; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public LocalDate getPostingDate() { return postingDate; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionDirection getDirection() { return direction; }
    public TransactionType getTransactionType() { return transactionType; }
    public TransactionPostingStatus getPostingStatus() { return postingStatus; }
    public String getRawDescription() { return rawDescription; }
    public String getNormalizedDescription() { return normalizedDescription; }
    public SourceAdapter getSource() { return source; }
    public String getSourceReference() { return sourceReference; }
    public String getTransactionFingerprint() { return transactionFingerprint; }
    public BigDecimal getClassificationConfidence() { return classificationConfidence; }
    public boolean isTransfer() { return transfer; }
    public boolean isReimbursement() { return reimbursement; }
    public boolean isStatementPayment() { return statementPayment; }
    public boolean isRecurring() { return recurring; }
}
