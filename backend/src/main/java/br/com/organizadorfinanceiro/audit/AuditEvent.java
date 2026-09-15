package br.com.organizadorfinanceiro.audit;

import java.util.Map;
import java.util.UUID;

import br.com.organizadorfinanceiro.shared.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_event", schema = "organizadorfinanceiro")
public class AuditEvent extends OwnedEntity {
    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "actor_type", nullable = false, length = 20)
    private String actorType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_value", columnDefinition = "jsonb")
    private Map<String, Object> afterValue;

    protected AuditEvent() {
    }

    public AuditEvent(UUID userId, String entityType, UUID entityId, String action,
                      Map<String, Object> afterValue) {
        super(userId);
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actorType = "USER";
        this.afterValue = afterValue;
    }
}
