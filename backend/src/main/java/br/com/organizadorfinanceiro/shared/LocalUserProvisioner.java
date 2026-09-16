package br.com.organizadorfinanceiro.shared;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.user-provisioning.mode", havingValue = "local")
class LocalUserProvisioner implements UserProvisioner {
    private final JdbcTemplate jdbcTemplate;

    LocalUserProvisioner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void ensureExists(UUID userId) {
        jdbcTemplate.update("insert into auth.users(id) values (?) on conflict (id) do nothing", userId);
    }
}
