package br.com.organizadorfinanceiro.imports.inter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import br.com.organizadorfinanceiro.cards.CreditCard;
import br.com.organizadorfinanceiro.imports.ImportDocumentStatus;
import br.com.organizadorfinanceiro.imports.ImportExecution;
import br.com.organizadorfinanceiro.shared.HashingService;
import br.com.organizadorfinanceiro.transactions.FinancialTransaction;
import br.com.organizadorfinanceiro.transactions.FinancialTransactionRepository;
import br.com.organizadorfinanceiro.transactions.RawTransaction;
import br.com.organizadorfinanceiro.transactions.RawTransactionRepository;
import br.com.organizadorfinanceiro.transactions.TransactionDirection;
import br.com.organizadorfinanceiro.transactions.TransactionPostingStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InterCardImportProcessorTest {
    @Test
    void importsOnlyNewRowsAndKeepsProjectedEntriesOutOfPostedLedger() throws Exception {
        UUID userId = UUID.fromString("6d5d4797-3df9-41ed-b5ca-0d9e606bc44e");
        UUID cardId = UUID.fromString("cf4b6372-9df5-4ea5-a9d8-27c7869498d8");
        CreditCard card = mock(CreditCard.class);
        ImportExecution execution = mock(ImportExecution.class);
        RawTransactionRepository rawRepository = mock(RawTransactionRepository.class);
        FinancialTransactionRepository transactionRepository = mock(FinancialTransactionRepository.class);
        InterCardFingerprint fingerprint = new InterCardFingerprint(new HashingService());
        InterCardStatement statement = parse("/inter/card-projected.fixture");
        when(card.getId()).thenReturn(cardId);
        when(card.getLastFourDigits()).thenReturn("5767");
        when(rawRepository.findExistingFingerprints(eq(userId), anyCollection())).thenReturn(Set.of());
        when(rawRepository.save(any(RawTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterCardImportProcessor processor = new InterCardImportProcessor(
                rawRepository, transactionRepository, fingerprint);
        InterCardImportProcessor.Result result = processor.process(
                userId, execution, card, ImportDocumentStatus.PROJECTED, statement);

        assertThat(result.detectedRows()).isEqualTo(1);
        assertThat(result.importedRows()).isEqualTo(1);
        ArgumentCaptor<FinancialTransaction> transaction = ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("321.09");
        assertThat(transaction.getValue().getDirection()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(transaction.getValue().getPostingStatus()).isEqualTo(TransactionPostingStatus.PROJECTED);
        assertThat(transaction.getValue().getPostingDate()).isNull();
        verify(execution).complete(1, 1, 0, 0);
    }

    @Test
    void countsPreviouslyImportedRowsWithoutCreatingCopies() throws Exception {
        UUID userId = UUID.fromString("6d5d4797-3df9-41ed-b5ca-0d9e606bc44e");
        UUID cardId = UUID.fromString("cf4b6372-9df5-4ea5-a9d8-27c7869498d8");
        CreditCard card = mock(CreditCard.class);
        ImportExecution execution = mock(ImportExecution.class);
        RawTransactionRepository rawRepository = mock(RawTransactionRepository.class);
        FinancialTransactionRepository transactionRepository = mock(FinancialTransactionRepository.class);
        InterCardFingerprint fingerprint = new InterCardFingerprint(new HashingService());
        InterCardStatement statement = parse("/inter/card-projected.fixture");
        String existing = fingerprint.create(cardId, statement.entries().getFirst(), 1);
        when(card.getId()).thenReturn(cardId);
        when(rawRepository.findExistingFingerprints(eq(userId), anyCollection())).thenReturn(Set.of(existing));

        InterCardImportProcessor processor = new InterCardImportProcessor(
                rawRepository, transactionRepository, fingerprint);
        InterCardImportProcessor.Result result = processor.process(
                userId, execution, card, ImportDocumentStatus.PROJECTED, statement);

        assertThat(result.importedRows()).isZero();
        assertThat(result.duplicateRows()).isEqualTo(1);
        verify(rawRepository, times(0)).save(any(RawTransaction.class));
        verify(transactionRepository, times(0)).save(any(FinancialTransaction.class));
        verify(execution).complete(1, 0, 1, 0);
    }

    private InterCardStatement parse(String path) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Recurso ausente: " + path);
            return new InterCardCsvParser().parse(input);
        }
    }
}
