package br.com.organizadorfinanceiro.imports;

import java.util.Map;

import br.com.organizadorfinanceiro.shared.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "import_warning", schema = "organizadorfinanceiro")
public class ImportWarning extends OwnedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_execution_id", nullable = false)
    private ImportExecution importExecution;

    @Column(name = "source_row_number")
    private Integer sourceRowNumber;

    @Column(name = "warning_code", nullable = false, length = 80)
    private String warningCode;

    @Column(name = "warning_message", nullable = false, length = 500)
    private String warningMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "warning_context", columnDefinition = "jsonb")
    private Map<String, Object> warningContext;

    protected ImportWarning() {
    }
}
