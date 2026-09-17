package br.com.organizadorfinanceiro.imports.inter;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import br.com.organizadorfinanceiro.accounts.FinancialAccount;
import br.com.organizadorfinanceiro.imports.ImportExecution;
import br.com.organizadorfinanceiro.transactions.FinancialTransaction;
import br.com.organizadorfinanceiro.transactions.FinancialTransactionRepository;
import br.com.organizadorfinanceiro.transactions.RawTransaction;
import br.com.organizadorfinanceiro.transactions.RawTransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class InterAccountImportProcessor {
    private final RawTransactionRepository rawRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final InterAccountFingerprint fingerprint;

    public InterAccountImportProcessor(RawTransactionRepository rawRepository,
                                       FinancialTransactionRepository transactionRepository,
                                       InterAccountFingerprint fingerprint) {
        this.rawRepository = rawRepository;
        this.transactionRepository = transactionRepository;
        this.fingerprint = fingerprint;
    }

    public Result process(UUID userId, ImportExecution execution, FinancialAccount account,
                          InterAccountStatement statement) {
        execution.markParsing();
        List<PreparedEntry> prepared = statement.entries().stream()
                .map(entry -> new PreparedEntry(entry, fingerprint.create(account.getId(), entry)))
                .toList();
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
            InterAccountStatement.Entry entry = preparedEntry.entry();
            RawTransaction raw = rawRepository.save(new RawTransaction(
                    userId, execution, entry.sourceRowNumber(), statement.accountNumber(),
                    entry.rawDate(), entry.description(), entry.rawAmount(), rawPayload(statement, entry),
                    preparedEntry.fingerprint()));
            transactionRepository.save(FinancialTransaction.fromAccountStatement(
                    userId, raw, account, entry.date(), entry.signedAmount(), entry.description(),
                    statement.accountNumber(), preparedEntry.fingerprint()));
            imported++;
        }
        execution.complete(prepared.size(), imported, duplicates, 0);
        return new Result(prepared.size(), imported, duplicates, 0);
    }

    private Map<String, Object> rawPayload(InterAccountStatement statement, InterAccountStatement.Entry entry) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountNumber", statement.accountNumber());
        payload.put("periodStart", statement.periodStart().toString());
        payload.put("periodEnd", statement.periodEnd().toString());
        payload.put("statementEndingBalance", statement.endingBalance());
        payload.put("date", entry.rawDate());
        payload.put("description", entry.description());
        payload.put("amount", entry.rawAmount());
        payload.put("balanceAfter", entry.rawBalance());
        return payload;
    }

    private record PreparedEntry(InterAccountStatement.Entry entry, String fingerprint) {}
    public record Result(int detectedRows, int importedRows, int duplicateRows, int warningCount) {}
}
