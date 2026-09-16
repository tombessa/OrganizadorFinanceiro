package br.com.organizadorfinanceiro.imports;

import java.time.Instant;
import java.util.UUID;

import br.com.organizadorfinanceiro.accounts.FinancialAccount;
import br.com.organizadorfinanceiro.cards.CreditCard;
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
@Table(name = "import_execution", schema = "organizadorfinanceiro")
public class ImportExecution extends OwnedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_file_id", nullable = false)
    private ImportFile importFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private FinancialAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ImportTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportExecutionStatus status;

    @Column(name = "detected_rows", nullable = false)
    private int detectedRows;

    @Column(name = "imported_rows", nullable = false)
    private int importedRows;

    @Column(name = "duplicate_rows", nullable = false)
    private int duplicateRows;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Column(name = "adapter_version", nullable = false, length = 40)
    private String adapterVersion;

    @Column(name = "rules_version", nullable = false, length = 40)
    private String rulesVersion;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_summary", length = 500)
    private String errorSummary;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected ImportExecution() {
    }

    public ImportExecution(UUID userId, ImportFile importFile, FinancialAccount account,
                           CreditCard creditCard, SourceAdapter adapter) {
        super(userId);
        this.importFile = importFile;
        this.account = account;
        this.creditCard = creditCard;
        this.targetType = adapter.targetType();
        this.status = ImportExecutionStatus.RECEIVED;
        this.adapterVersion = adapter.version();
        this.rulesVersion = "foundation-v1";
        this.startedAt = Instant.now();
    }

    public ImportFile getImportFile() { return importFile; }
    public FinancialAccount getAccount() { return account; }
    public CreditCard getCreditCard() { return creditCard; }
    public ImportTargetType getTargetType() { return targetType; }
    public ImportExecutionStatus getStatus() { return status; }
    public int getDetectedRows() { return detectedRows; }
    public int getImportedRows() { return importedRows; }
    public int getDuplicateRows() { return duplicateRows; }
    public int getWarningCount() { return warningCount; }
    public String getAdapterVersion() { return adapterVersion; }
    public String getRulesVersion() { return rulesVersion; }
    public String getErrorCode() { return errorCode; }
    public String getErrorSummary() { return errorSummary; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
