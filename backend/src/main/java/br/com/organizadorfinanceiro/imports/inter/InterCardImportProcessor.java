package br.com.organizadorfinanceiro.imports.inter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import br.com.organizadorfinanceiro.cards.CreditCard;
import br.com.organizadorfinanceiro.imports.ImportDocumentStatus;
import br.com.organizadorfinanceiro.imports.ImportExecution;
import br.com.organizadorfinanceiro.transactions.FinancialTransaction;
import br.com.organizadorfinanceiro.transactions.FinancialTransactionRepository;
import br.com.organizadorfinanceiro.transactions.RawTransaction;
import br.com.organizadorfinanceiro.transactions.RawTransactionRepository;
import br.com.organizadorfinanceiro.transactions.TransactionPostingStatus;
import org.springframework.stereotype.Service;

@Service
public class InterCardImportProcessor {
    private final RawTransactionRepository rawRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final InterCardFingerprint fingerprint;

    public InterCardImportProcessor(RawTransactionRepository rawRepository,
                                    FinancialTransactionRepository transactionRepository,
                                    InterCardFingerprint fingerprint) {
        this.rawRepository = rawRepository;
        this.transactionRepository = transactionRepository;
        this.fingerprint = fingerprint;
    }

    public Result process(UUID userId, ImportExecution execution, CreditCard card,
                          ImportDocumentStatus documentStatus, InterCardStatement statement) {
        execution.markParsing();
        Map<String, Integer> occurrences = new HashMap<>();
        List<PreparedEntry> prepared = statement.entries().stream().map(entry -> {
            int occurrence = occurrences.merge(fingerprint.businessKey(entry), 1, Integer::sum);
            return new PreparedEntry(entry, fingerprint.create(card.getId(), entry, occurrence));
        }).toList();
        Set<String> requested = new HashSet<>();
        prepared.forEach(entry -> requested.add(entry.fingerprint()));
        Set<String> existing = new HashSet<>(rawRepository.findExistingFingerprints(userId, requested));

        int imported = 0;
        int duplicates = 0;
        for (PreparedEntry preparedEntry : prepared) {
            if (!existing.add(preparedEntry.fingerprint())) {
                duplicates++;
                continue;
            }
            InterCardStatement.Entry entry = preparedEntry.entry();
            RawTransaction raw = rawRepository.save(new RawTransaction(
                    userId, execution, entry.sourceRowNumber(), card.getLastFourDigits(),
                    entry.rawDate(), entry.description(), entry.rawAmount(),
                    rawPayload(documentStatus, entry), preparedEntry.fingerprint()));
            transactionRepository.save(FinancialTransaction.fromCardStatement(
                    userId, raw, card, entry.date(), entry.signedAmount(), entry.description(),
                    card.getLastFourDigits(), preparedEntry.fingerprint(), postingStatus(documentStatus)));
            imported++;
        }
        execution.complete(prepared.size(), imported, duplicates, 0);
        return new Result(prepared.size(), imported, duplicates, 0);
    }

    private TransactionPostingStatus postingStatus(ImportDocumentStatus status) {
        return status == ImportDocumentStatus.PROJECTED
                ? TransactionPostingStatus.PROJECTED : TransactionPostingStatus.POSTED;
    }

    private Map<String, Object> rawPayload(ImportDocumentStatus documentStatus, InterCardStatement.Entry entry) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentStatus", documentStatus.name());
        payload.put("date", entry.rawDate());
        payload.put("description", entry.description());
        payload.put("category", entry.category());
        payload.put("type", entry.type());
        payload.put("amount", entry.rawAmount());
        if (entry.installment()) {
            payload.put("installmentNumber", entry.installmentNumber());
            payload.put("installmentTotal", entry.installmentTotal());
        }
        return payload;
    }

    private record PreparedEntry(InterCardStatement.Entry entry, String fingerprint) {}
    public record Result(int detectedRows, int importedRows, int duplicateRows, int warningCount) {}
}
