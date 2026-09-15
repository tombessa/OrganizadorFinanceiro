package br.com.organizadorfinanceiro.cards;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.organizadorfinanceiro.audit.AuditService;
import br.com.organizadorfinanceiro.accounts.FinancialAccount;
import br.com.organizadorfinanceiro.accounts.FinancialAccountRepository;
import br.com.organizadorfinanceiro.institutions.FinancialInstitution;
import br.com.organizadorfinanceiro.institutions.FinancialInstitutionRepository;
import br.com.organizadorfinanceiro.shared.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@RequestMapping("/api/cards")
public class CardController {
    private final CreditCardRepository repository;
    private final FinancialInstitutionRepository institutionRepository;
    private final FinancialAccountRepository accountRepository;
    private final AuthenticatedUser authenticatedUser;
    private final AuditService auditService;

    public CardController(CreditCardRepository repository,
                          FinancialInstitutionRepository institutionRepository,
                          FinancialAccountRepository accountRepository,
                          AuthenticatedUser authenticatedUser,
                          AuditService auditService) {
        this.repository = repository;
        this.institutionRepository = institutionRepository;
        this.accountRepository = accountRepository;
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
        FinancialAccount account = request.paymentAccountId() == null ? null
                : accountRepository.findByIdAndUserId(request.paymentAccountId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta de pagamento não encontrada"));
        CreditCard card = repository.save(new CreditCard(userId, institution, account, request.name(),
                request.lastFourDigits(), request.closingDay(), request.dueDay()));
        auditService.created(userId, "CREDIT_CARD", card.getId(), Map.of(
                "institutionId", institution.getId().toString(),
                "name", card.getName(),
                "lastFourDigits", card.getLastFourDigits(),
                "closingDay", card.getClosingDay(),
                "dueDay", card.getDueDay()));
        return Response.from(card);
    }

    public record CreateRequest(@NotNull UUID institutionId, UUID paymentAccountId,
                                @NotBlank @Size(max = 120) String name,
                                @NotBlank @Pattern(regexp = "\\d{4}") String lastFourDigits,
                                @Min(1) @Max(31) short closingDay,
                                @Min(1) @Max(31) short dueDay) {}

    public record Response(UUID id, UUID institutionId, UUID paymentAccountId, String name,
                           String lastFourDigits, short closingDay, short dueDay, boolean active) {
        static Response from(CreditCard value) {
            return new Response(value.getId(), value.getInstitution().getId(),
                    value.getPaymentAccount() == null ? null : value.getPaymentAccount().getId(),
                    value.getName(), value.getLastFourDigits(), value.getClosingDay(), value.getDueDay(), value.isActive());
        }
    }
}
