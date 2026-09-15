package br.com.organizadorfinanceiro.imports;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import br.com.organizadorfinanceiro.audit.AuditService;
import br.com.organizadorfinanceiro.shared.HashingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportRegistrationService {
    private final ImportFileRepository repository;
    private final HashingService hashingService;
    private final AuditService auditService;

    public ImportRegistrationService(ImportFileRepository repository, HashingService hashingService,
                                     AuditService auditService) {
        this.repository = repository;
        this.hashingService = hashingService;
        this.auditService = auditService;
    }

    @Transactional
    public Result register(UUID userId, SourceAdapter adapter, MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) throw new IllegalArgumentException("O arquivo está vazio");
        String filename = safeFilename(multipartFile.getOriginalFilename());
        validateExtension(adapter, filename);
        String hash;
        try (InputStream input = multipartFile.getInputStream()) {
            hash = hashingService.sha256(input);
        }
        return repository.findByUserIdAndSourceAdapterAndContentHash(userId, adapter, hash)
                .map(existing -> new Result(existing, true))
                .orElseGet(() -> registerNew(userId, adapter, multipartFile, filename, hash));
    }

    private Result registerNew(UUID userId, SourceAdapter adapter, MultipartFile multipartFile,
                               String filename, String hash) {
        ImportFile imported = repository.save(new ImportFile(userId, adapter, filename,
                contentType(multipartFile), multipartFile.getSize(), hash));
        auditService.created(userId, "IMPORT_FILE", imported.getId(), Map.of(
                "sourceAdapter", adapter.name(),
                "filename", filename,
                "byteSize", multipartFile.getSize(),
                "sha256", hash));
        return new Result(imported, false);
    }

    private String safeFilename(String value) {
        String filename = value == null ? "arquivo" : value.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1).trim();
        if (filename.isBlank() || filename.length() > 255) throw new IllegalArgumentException("Nome de arquivo inválido");
        return filename;
    }

    private String contentType(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
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

    public record Result(ImportFile file, boolean duplicate) {}
}
