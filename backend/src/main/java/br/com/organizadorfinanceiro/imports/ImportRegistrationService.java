package br.com.organizadorfinanceiro.imports;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.organizadorfinanceiro.accounts.FinancialAccount;
import br.com.organizadorfinanceiro.accounts.FinancialAccountRepository;
import br.com.organizadorfinanceiro.audit.AuditService;
import br.com.organizadorfinanceiro.cards.CreditCard;
import br.com.organizadorfinanceiro.cards.CreditCardRepository;
import br.com.organizadorfinanceiro.imports.inter.InterAccountCsvParser;
import br.com.organizadorfinanceiro.imports.inter.InterAccountImportProcessor;
import br.com.organizadorfinanceiro.imports.inter.InterAccountStatement;
import br.com.organizadorfinanceiro.imports.inter.InterCardCsvParser;
import br.com.organizadorfinanceiro.imports.inter.InterCardImportProcessor;
import br.com.organizadorfinanceiro.imports.inter.InterCardStatement;
import br.com.organizadorfinanceiro.imports.itau.ItauCardImportProcessor;
import br.com.organizadorfinanceiro.imports.itau.ItauCardStatement;
import br.com.organizadorfinanceiro.imports.itau.ItauCardXlsxParser;
import br.com.organizadorfinanceiro.shared.HashingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportRegistrationService {
    private final ImportFileRepository repository;
    private final ImportExecutionRepository executionRepository;
    private final FinancialAccountRepository accountRepository;
    private final CreditCardRepository cardRepository;
    private final ImportFileStorage storage;
    private final HashingService hashingService;
    private final AuditService auditService;
    private final InterAccountCsvParser interAccountParser;
    private final InterAccountImportProcessor interAccountProcessor;
    private final InterCardCsvParser interCardParser;
    private final InterCardImportProcessor interCardProcessor;
    private final ItauCardXlsxParser itauCardParser;
    private final ItauCardImportProcessor itauCardProcessor;

    public ImportRegistrationService(ImportFileRepository repository,
                                     ImportExecutionRepository executionRepository,
                                     FinancialAccountRepository accountRepository,
                                     CreditCardRepository cardRepository,
                                     ImportFileStorage storage,
                                     HashingService hashingService,
                                     AuditService auditService,
                                     InterAccountCsvParser interAccountParser,
                                     InterAccountImportProcessor interAccountProcessor,
                                     InterCardCsvParser interCardParser,
                                     InterCardImportProcessor interCardProcessor,
                                     ItauCardXlsxParser itauCardParser,
                                     ItauCardImportProcessor itauCardProcessor) {
        this.repository = repository;
        this.executionRepository = executionRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.storage = storage;
        this.hashingService = hashingService;
        this.auditService = auditService;
        this.interAccountParser = interAccountParser;
        this.interAccountProcessor = interAccountProcessor;
        this.interCardParser = interCardParser;
        this.interCardProcessor = interCardProcessor;
        this.itauCardParser = itauCardParser;
        this.itauCardProcessor = itauCardProcessor;
    }

    @Transactional
    public Result register(UUID userId, SourceAdapter adapter, UUID targetId,
                           ImportDocumentStatus documentStatus, MultipartFile multipartFile,
                           ImportFileStorage.Credentials credentials) throws IOException {
        if (multipartFile.isEmpty()) throw new IllegalArgumentException("O arquivo está vazio");
        String filename = safeFilename(multipartFile.getOriginalFilename());
        validateExtension(adapter, filename);
        Target target = findTarget(userId, adapter, targetId);
        validateDocumentStatus(adapter, documentStatus);
        ItauCardStatement itauCardStatement = null;
        if (adapter == SourceAdapter.ITAU_CARD_XLSX) {
            try (InputStream input = multipartFile.getInputStream()) {
                itauCardStatement = itauCardParser.parse(input);
                documentStatus = itauCardStatement.documentStatus();
            }
        }
        String hash;
        try (InputStream input = multipartFile.getInputStream()) {
            hash = hashingService.sha256(input);
        }
        Optional<ImportFile> existing = repository.findByUserIdAndSourceAdapterAndContentHash(userId, adapter, hash);
        if (existing.isPresent()) {
            ImportExecution execution = executionRepository
                    .findFirstByUserIdAndImportFileIdOrderByStartedAtDesc(userId, existing.get().getId())
                    .orElse(null);
            if (adapter.targetType() == ImportTargetType.CARD && execution != null
                    && execution.getDocumentStatus() != documentStatus) {
                throw new IllegalArgumentException("Este arquivo já foi registrado com outra situação de fatura");
            }
            return new Result(existing.get(), execution, true);
        }
        InterAccountStatement interAccountStatement = null;
        InterCardStatement interCardStatement = null;
        if (adapter == SourceAdapter.INTER_ACCOUNT_CSV) {
            try (InputStream input = multipartFile.getInputStream()) {
                interAccountStatement = interAccountParser.parse(input);
            }
        }
        if (adapter == SourceAdapter.INTER_CARD_CSV) {
            try (InputStream input = multipartFile.getInputStream()) {
                interCardStatement = interCardParser.parse(input);
            }
        }
        return registerNew(userId, adapter, target, multipartFile, filename, hash,
                credentials, documentStatus, interAccountStatement, interCardStatement, itauCardStatement);
    }

    private Result registerNew(UUID userId, SourceAdapter adapter, Target target, MultipartFile multipartFile,
                               String filename, String hash, ImportFileStorage.Credentials credentials,
                               ImportDocumentStatus documentStatus,
                               InterAccountStatement interAccountStatement,
                               InterCardStatement interCardStatement,
                               ItauCardStatement itauCardStatement) throws IOException {
        ImportFileStorage.StoredObject object = storage.store(userId, adapter, hash, multipartFile, credentials);
        try {
            ImportFile imported = repository.save(new ImportFile(userId, adapter, filename,
                    contentType(adapter), multipartFile.getSize(), hash, object.bucket(), object.path()));
            ImportExecution execution = executionRepository.save(new ImportExecution(
                    userId, imported, target.account(), target.card(), adapter, documentStatus));
            if (interAccountStatement != null) {
                interAccountProcessor.process(userId, execution, target.account(), interAccountStatement);
            }
            if (interCardStatement != null) {
                interCardProcessor.process(userId, execution, target.card(), documentStatus, interCardStatement);
            }
            if (itauCardStatement != null) {
                itauCardProcessor.process(userId, execution, target.card(), itauCardStatement);
            }
            auditService.created(userId, "IMPORT_FILE", imported.getId(), Map.of(
                    "sourceAdapter", adapter.name(),
                    "filename", filename,
                    "byteSize", multipartFile.getSize(),
                    "sha256", hash,
                    "storageStatus", imported.getStorageStatus().name()));
            auditService.created(userId, "IMPORT_EXECUTION", execution.getId(), Map.of(
                    "importFileId", imported.getId().toString(),
                    "targetType", execution.getTargetType().name(),
                    "documentStatus", execution.getDocumentStatus() == null ? "NOT_APPLICABLE" : execution.getDocumentStatus().name(),
                    "status", execution.getStatus().name(),
                    "adapterVersion", execution.getAdapterVersion(),
                    "rulesVersion", execution.getRulesVersion(),
                    "detectedRows", execution.getDetectedRows(),
                    "importedRows", execution.getImportedRows(),
                    "duplicateRows", execution.getDuplicateRows(),
                    "warningCount", execution.getWarningCount()));
            return new Result(imported, execution, false);
        } catch (RuntimeException exception) {
            storage.delete(object, credentials);
            throw exception;
        }
    }

    private Target findTarget(UUID userId, SourceAdapter adapter, UUID targetId) {
        return switch (adapter.targetType()) {
            case ACCOUNT -> {
                if (targetId == null) throw new IllegalArgumentException("Selecione a conta do arquivo");
                FinancialAccount account = accountRepository.findByIdAndUserId(targetId, userId)
                        .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
                yield new Target(account, null);
            }
            case CARD -> {
                if (targetId == null) throw new IllegalArgumentException("Selecione o cartão do arquivo");
                CreditCard card = cardRepository.findByIdAndUserId(targetId, userId)
                        .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado"));
                yield new Target(null, card);
            }
            case PAYROLL -> {
                if (targetId != null) throw new IllegalArgumentException("Contracheque não aceita conta ou cartão");
                yield new Target(null, null);
            }
        };
    }

    private void validateDocumentStatus(SourceAdapter adapter, ImportDocumentStatus documentStatus) {
        if (adapter == SourceAdapter.INTER_CARD_CSV && documentStatus == null) {
            throw new IllegalArgumentException("Informe se a fatura é efetiva ou projetada");
        }
        if (adapter == SourceAdapter.INTER_CARD_CSV && documentStatus != ImportDocumentStatus.POSTED
                && documentStatus != ImportDocumentStatus.PROJECTED) {
            throw new IllegalArgumentException("A fatura Inter aceita somente situação efetiva ou projetada");
        }
        if (adapter == SourceAdapter.ITAU_CARD_XLSX && documentStatus != null) {
            throw new IllegalArgumentException("A situação da fatura Itaú é detectada automaticamente");
        }
        if (adapter.targetType() != ImportTargetType.CARD && documentStatus != null) {
            throw new IllegalArgumentException("Situação do documento só é aceita para faturas de cartão");
        }
    }

    private String safeFilename(String value) {
        String filename = value == null ? "arquivo" : value.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1).trim();
        if (filename.isBlank() || filename.length() > 255) throw new IllegalArgumentException("Nome de arquivo inválido");
        return filename;
    }

    private String contentType(SourceAdapter adapter) {
        return switch (adapter) {
            case INTER_ACCOUNT_CSV, INTER_CARD_CSV -> "text/csv";
            case ITAU_CARD_XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case SANTANDER_ACCOUNT_PDF, PAYROLL_PDF -> "application/pdf";
        };
    }

    private void validateExtension(SourceAdapter adapter, String filename) {
        String lower = filename.toLowerCase();
        boolean valid = switch (adapter) {
            case INTER_ACCOUNT_CSV, INTER_CARD_CSV -> lower.endsWith(".csv");
            case ITAU_CARD_XLSX -> lower.endsWith(".xlsx");
            case SANTANDER_ACCOUNT_PDF, PAYROLL_PDF -> lower.endsWith(".pdf");
        };
        if (!valid) throw new IllegalArgumentException("Extensão incompatível com o adaptador " + adapter);
    }

    private record Target(FinancialAccount account, CreditCard card) {}
    public record Result(ImportFile file, ImportExecution execution, boolean duplicate) {}
}
