package br.com.organizadorfinanceiro.institutions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.organizadorfinanceiro.audit.AuditService;
import br.com.organizadorfinanceiro.shared.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {
    private final FinancialInstitutionRepository repository;
    private final AuthenticatedUser authenticatedUser;
    private final AuditService auditService;

    public InstitutionController(FinancialInstitutionRepository repository, AuthenticatedUser authenticatedUser,
                                 AuditService auditService) {
        this.repository = repository;
        this.authenticatedUser = authenticatedUser;
        this.auditService = auditService;
    }

    @GetMapping
    List<Response> list(Authentication authentication) {
        return repository.findAllByUserIdOrderByName(authenticatedUser.id(authentication)).stream().map(Response::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    Response create(@Valid @RequestBody CreateRequest request, Authentication authentication) {
        UUID userId = authenticatedUser.id(authentication);
        FinancialInstitution institution = repository.save(new FinancialInstitution(
                userId, request.code(), request.name()));
        auditService.created(userId, "FINANCIAL_INSTITUTION", institution.getId(),
                Map.of("code", institution.getCode(), "name", institution.getName()));
        return Response.from(institution);
    }

    public record CreateRequest(@NotBlank @Size(max = 40) String code,
                                @NotBlank @Size(max = 120) String name) {}

    public record Response(UUID id, String code, String name, boolean active) {
        static Response from(FinancialInstitution value) {
            return new Response(value.getId(), value.getCode(), value.getName(), value.isActive());
        }
    }
}
