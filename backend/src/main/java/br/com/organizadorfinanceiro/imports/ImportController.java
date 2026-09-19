package br.com.organizadorfinanceiro.imports;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import br.com.organizadorfinanceiro.shared.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
public class ImportController {
    private final ImportRegistrationService service;
    private final AuthenticatedUser authenticatedUser;

    public ImportController(ImportRegistrationService service, AuthenticatedUser authenticatedUser) {
        this.service = service;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping(path = "/register", consumes = "multipart/form-data")
    ResponseEntity<Response> register(@RequestParam SourceAdapter adapter,
                                      @RequestParam(required = false) UUID targetId,
                                      @RequestParam(required = false) ImportDocumentStatus documentStatus,
                                      @RequestPart("file") MultipartFile file,
                                      @RequestHeader(name = "X-Supabase-Api-Key", required = false) String apiKey,
                                      Authentication authentication) throws IOException {
        ImportRegistrationService.Result result = service.register(
                authenticatedUser.id(authentication), adapter, targetId, documentStatus, file,
                new ImportFileStorage.Credentials(authenticatedUser.accessToken(authentication), apiKey));
        ImportFile registered = result.file();
        ImportExecution execution = result.execution();
        Response response = new Response(registered.getId(), registered.getSourceAdapter(), registered.getOriginalFilename(),
                registered.getByteSize(), registered.getContentHash(), registered.getReceivedAt(),
                registered.getStorageStatus(), execution == null ? null : execution.getId(),
                execution == null ? null : execution.getDocumentStatus(),
                execution == null ? ImportExecutionStatus.DUPLICATE : execution.getStatus(),
                execution == null ? 0 : execution.getDetectedRows(),
                execution == null ? 0 : execution.getImportedRows(),
                execution == null ? 0 : execution.getDuplicateRows(),
                execution == null ? 0 : execution.getWarningCount(), result.duplicate(),
                result.duplicate() ? HttpStatus.OK.value() : HttpStatus.CREATED.value());
        return ResponseEntity.status(result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    public record Response(UUID id, SourceAdapter adapter, String filename, long byteSize,
                           String sha256, Instant receivedAt, StorageStatus storageStatus,
                           UUID executionId, ImportDocumentStatus documentStatus,
                           ImportExecutionStatus executionStatus,
                           int detectedRows, int importedRows, int duplicateRows, int warningCount,
                           boolean duplicate, int suggestedHttpStatus) {}
}
