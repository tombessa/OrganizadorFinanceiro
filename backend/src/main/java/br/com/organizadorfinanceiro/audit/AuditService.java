package br.com.organizadorfinanceiro.audit;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public void created(UUID userId, String entityType, UUID entityId, Map<String, Object> afterValue) {
        repository.save(new AuditEvent(userId, entityType, entityId, "CREATED", afterValue));
    }
}
