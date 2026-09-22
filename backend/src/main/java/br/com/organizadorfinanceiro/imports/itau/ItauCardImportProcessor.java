package br.com.organizadorfinanceiro.imports.itau;

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
import br.com.organizadorfinanceiro.imports.SourceAdapter;
import br.com.organizadorfinanceiro.transactions.FinancialTransaction;
import br.com.organizadorfinanceiro.transactions.FinancialTransactionRepository;
import br.com.organizadorfinanceiro.transactions.RawTransaction;
import br.com.organizadorfinanceiro.transactions.RawTransactionRepository;
import br.com.organizadorfinanceiro.transactions.TransactionPostingStatus;
import org.springframework.stereotype.Service;

@Service
public class ItauCardImportProcessor {
    private final RawTransactionRepository rawRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final ItauCardFingerprint fingerprint;

    public ItauCardImportProcessor(RawTransactionRepository rawRepository,
                                   FinancialTransactionRepository transactionRepository,
                                   ItauCardFingerprint fingerprint) {
        this.rawRepository = rawRepository;
        this.transactionRepository = transactionRepository;
        this.fingerprint = fingerprint;
    }

    public Result process(UUID userId, ImportExecution execution, CreditCard card,
                          ItauCardStatement statement) {
        if (!card.getLastFourDigits().equals(statement.masterCardLastFourDigits())) {
            throw new IllegalArgumentException("O cartão selecionado não corresponde ao final "
                    + statement.masterCardLastFourDigits() + " da fatura Itaú");
        }
        execution.markParsing();
        Map<String, Integer> occurrences = new HashMap<>();
        List<PreparedEntry> prepared = statement.entries().stream().map(entry -> {
            int occurrence = occurrences.merge(fingerprint.businessKey(entry), 1, Integer::sum);
            return new PreparedEntry(entry, fingerprint.create(card.getId(), statement, entry, occurrence));
        }).toList();
        Set<String> requested = new HashSet<>();
        prepared.forEach(entry -> requested.add(entry.fingerprint()));
        Set<String> existing = new HashSet<>(rawRepository.findExistingFingerprints(userId, requested));

        int imported = 0;
        int duplicates = 0;
        for (PreparedEntry preparedEntry : prepared) {
            if (!existing.add(preparedEntry.fingerprint())) {
                promoteProjection(userId, preparedEntry.fingerprint(), statement.documentStatus());
                duplicates++;
                continue;
            }
            ItauCardStatement.Entry entry = preparedEntry.entry();
            RawTransaction raw = rawRepository.save(new RawTransaction(
                    userId, execution, entry.sourceRowNumber(), entry.usedCardLastFourDigits(),
                    entry.date().toString(), entry.description(), entry.signedAmount().toPlainString(),
                    rawPayload(statement, entry), preparedEntry.fingerprint()));
            transactionRepository.save(FinancialTransaction.fromCardStatement(
                    userId, raw, card, entry.date(), entry.signedAmount(), entry.description(),
                    entry.usedCardLastFourDigits(), preparedEntry.fingerprint(), postingStatus(statement.documentStatus()),
                    SourceAdapter.ITAU_CARD_XLSX, entry.statementPayment()));
            imported++;
        }
        execution.complete(prepared.size(), imported, duplicates, 0);
        return new Result(prepared.size(), imported, duplicates, 0);
    }

    private void promoteProjection(UUID userId, String transactionFingerprint, ImportDocumentStatus documentStatus) {
        if (documentStatus == ImportDocumentStatus.PROJECTED) return;
        transactionRepository.findByUserIdAndTransactionFingerprint(userId, transactionFingerprint)
                .filter(transaction -> transaction.getPostingStatus() == TransactionPostingStatus.PROJECTED)
                .ifPresent(FinancialTransaction::promoteToPosted);
    }

    private TransactionPostingStatus postingStatus(ImportDocumentStatus status) {
        return status == ImportDocumentStatus.PROJECTED
                ? TransactionPostingStatus.PROJECTED : TransactionPostingStatus.POSTED;
    }

    private Map<String, Object> rawPayload(ItauCardStatement statement, ItauCardStatement.Entry entry) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentStatus", statement.documentStatus().name());
        payload.put("competence", statement.competence().toString());
        payload.put("dueDate", statement.dueDate().toString());
        payload.put("displayedTotal", statement.displayedTotal().toPlainString());
        payload.put("date", entry.date().toString());
        payload.put("description", entry.description());
        payload.put("amount", entry.signedAmount().toPlainString());
        payload.put("usedCardLastFourDigits", entry.usedCardLastFourDigits());
        payload.put("cardType", entry.cardType());
        payload.put("statementPayment", entry.statementPayment());
        if (entry.installment()) {
            payload.put("installmentNumber", entry.installmentNumber());
            payload.put("installmentTotal", entry.installmentTotal());
        }
        return payload;
    }

    private record PreparedEntry(ItauCardStatement.Entry entry, String fingerprint) {}
    public record Result(int detectedRows, int importedRows, int duplicateRows, int warningCount) {}
}
