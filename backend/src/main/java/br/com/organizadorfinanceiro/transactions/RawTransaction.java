package br.com.organizadorfinanceiro.transactions;

import java.util.Map;
import java.util.UUID;

import br.com.organizadorfinanceiro.imports.ImportExecution;
import br.com.organizadorfinanceiro.shared.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Immutable
@Table(name = "raw_transaction", schema = "organizadorfinanceiro")
public class RawTransaction extends OwnedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_execution_id", nullable = false)
    private ImportExecution importExecution;

    @Column(name = "source_row_number", nullable = false)
    private int sourceRowNumber;

    @Column(name = "source_reference", length = 255)
    private String sourceReference;

    @Column(name = "raw_date", length = 80)
    private String rawDate;

    @Column(name = "raw_description")
    private String rawDescription;

    @Column(name = "raw_amount", length = 80)
    private String rawAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawPayload;

    @Column(name = "raw_fingerprint", nullable = false, length = 64)
    private String rawFingerprint;

    protected RawTransaction() {
    }

    public RawTransaction(UUID userId, ImportExecution importExecution, int sourceRowNumber,
                          String sourceReference, String rawDate, String rawDescription,
                          String rawAmount, Map<String, Object> rawPayload, String rawFingerprint) {
        super(userId);
        this.importExecution = importExecution;
        this.sourceRowNumber = sourceRowNumber;
        this.sourceReference = sourceReference;
        this.rawDate = rawDate;
        this.rawDescription = rawDescription;
        this.rawAmount = rawAmount;
        this.rawPayload = Map.copyOf(rawPayload);
        this.rawFingerprint = rawFingerprint;
    }

    public ImportExecution getImportExecution() { return importExecution; }
    public int getSourceRowNumber() { return sourceRowNumber; }
    public String getSourceReference() { return sourceReference; }
    public String getRawDate() { return rawDate; }
    public String getRawDescription() { return rawDescription; }
    public String getRawAmount() { return rawAmount; }
    public Map<String, Object> getRawPayload() { return rawPayload; }
    public String getRawFingerprint() { return rawFingerprint; }
}
