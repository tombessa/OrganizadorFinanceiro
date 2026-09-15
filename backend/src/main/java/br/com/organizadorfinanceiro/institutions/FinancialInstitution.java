package br.com.organizadorfinanceiro.institutions;

import java.util.UUID;

import br.com.organizadorfinanceiro.shared.OwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "financial_institution", schema = "organizadorfinanceiro")
public class FinancialInstitution extends OwnedEntity {
    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    protected FinancialInstitution() {
    }

    public FinancialInstitution(UUID userId, String code, String name) {
        super(userId);
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}

