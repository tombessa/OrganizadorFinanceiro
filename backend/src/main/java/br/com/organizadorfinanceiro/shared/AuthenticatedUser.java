package br.com.organizadorfinanceiro.shared;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {
    private final UserProvisioner userProvisioner;

    public AuthenticatedUser(UserProvisioner userProvisioner) {
        this.userProvisioner = userProvisioner;
    }

    public UUID id(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Usuário autenticado não encontrado");
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        userProvisioner.ensureExists(userId);
        return userId;
    }

    public String accessToken(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Usuário autenticado não encontrado");
        }
        return jwt.getTokenValue();
    }
}
