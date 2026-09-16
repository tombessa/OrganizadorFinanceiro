package br.com.organizadorfinanceiro.shared;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.user-provisioning.mode", havingValue = "none", matchIfMissing = true)
class NoOpUserProvisioner implements UserProvisioner {
    @Override
    public void ensureExists(UUID userId) {
        // Supabase Auth owns auth.users in production.
    }
}
