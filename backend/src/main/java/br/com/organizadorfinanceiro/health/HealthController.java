package br.com.organizadorfinanceiro.health;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    Map<String, Object> health() {
        return Map.of("status", "UP", "service", "organizador-financeiro-backend", "timestamp", Instant.now());
    }
}

