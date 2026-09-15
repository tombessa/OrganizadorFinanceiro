package br.com.organizadorfinanceiro.shared;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {
    public UUID id(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Usuário autenticado não encontrado");
        }
        return UUID.fromString(jwt.getSubject());
    }
}

