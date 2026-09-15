package br.com.organizadorfinanceiro.accounts;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.organizadorfinanceiro.audit.AuditService;
import br.com.organizadorfinanceiro.institutions.FinancialInstitution;
import br.com.organizadorfinanceiro.institutions.FinancialInstitutionRepository;
import br.com.organizadorfinanceiro.shared.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final FinancialAccountRepository repository;
    private final FinancialInstitutionRepository institutionRepository;
    private final AuthenticatedUser authenticatedUser;
    private final AuditService auditService;

    public AccountController(FinancialAccountRepository repository,
                             FinancialInstitutionRepository institutionRepository,
                             AuthenticatedUser authenticatedUser,
                             AuditService auditService) {
        this.repository = repository;
        this.institutionRepository = institutionRepository;
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
        FinancialInstitution institution = institutionRepository.findByIdAndUserId(request.institutionId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instituição não encontrada"));
        FinancialAccount account = repository.save(new FinancialAccount(userId, institution, request.name(),
                request.accountType(), request.overdraftLimit()));
        auditService.created(userId, "FINANCIAL_ACCOUNT", account.getId(), Map.of(
                "institutionId", institution.getId().toString(),
                "name", account.getName(),
                "accountType", account.getAccountType().name(),
                "overdraftLimit", account.getOverdraftLimit()));
        return Response.from(account);
    }

    public record CreateRequest(@NotNull UUID institutionId,
                                @NotBlank @Size(max = 120) String name,
                                @NotNull AccountType accountType,
                                @DecimalMin("0.00") BigDecimal overdraftLimit) {}

    public record Response(UUID id, UUID institutionId, String name, AccountType accountType,
                           String currency, BigDecimal overdraftLimit, boolean active) {
        static Response from(FinancialAccount value) {
            return new Response(value.getId(), value.getInstitution().getId(), value.getName(),
                    value.getAccountType(), value.getCurrency(), value.getOverdraftLimit(), value.isActive());
        }
    }
}
