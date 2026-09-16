package br.com.organizadorfinanceiro.shared;

import java.util.UUID;

public interface UserProvisioner {
    void ensureExists(UUID userId);
}
