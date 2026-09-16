package br.com.organizadorfinanceiro.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticatedUserTest {
    @Test
    void shouldReadUserIdFromJwtSubject() {
        UUID id = UUID.randomUUID();
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of("sub", id.toString(), "aud", List.of("authenticated")));

        UUID actual = new AuthenticatedUser(userId -> { }).id(new JwtAuthenticationToken(jwt));

        assertThat(actual).isEqualTo(id);
    }
}
