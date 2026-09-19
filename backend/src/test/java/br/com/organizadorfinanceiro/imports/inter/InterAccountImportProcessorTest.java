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

import br.com.organizadorfinanceiro.accounts.FinancialAccount;
import br.com.organizadorfinanceiro.imports.ImportExecution;
import br.com.organizadorfinanceiro.shared.HashingService;
import br.com.organizadorfinanceiro.transactions.FinancialTransaction;
import br.com.organizadorfinanceiro.transactions.FinancialTransactionRepository;
import br.com.organizadorfinanceiro.transactions.RawTransaction;
import br.com.organizadorfinanceiro.transactions.RawTransactionRepository;
import br.com.organizadorfinanceiro.transactions.TransactionPostingStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InterAccountImportProcessorTest {
    @Test
    void importsOnlyNewRowsAndCompletesExecutionWithReconciledCounts() throws Exception {
        UUID userId = UUID.fromString("6d5d4797-3df9-41ed-b5ca-0d9e606bc44e");
        UUID accountId = UUID.fromString("d415325d-fd7f-4cc5-9164-1f63d398501e");
        FinancialAccount account = mock(FinancialAccount.class);
        ImportExecution execution = mock(ImportExecution.class);
        RawTransactionRepository rawRepository = mock(RawTransactionRepository.class);
        FinancialTransactionRepository transactionRepository = mock(FinancialTransactionRepository.class);
        InterAccountFingerprint fingerprint = new InterAccountFingerprint(new HashingService());
        InterAccountStatement statement = parse("/inter/account-short.fixture");
        when(account.getId()).thenReturn(accountId);
        when(rawRepository.findExistingFingerprints(eq(userId), anyCollection()))
                .thenReturn(Set.of(fingerprint.create(accountId, statement.entries().get(0))));
        when(rawRepository.save(any(RawTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterAccountImportProcessor processor = new InterAccountImportProcessor(
                rawRepository, transactionRepository, fingerprint);
        InterAccountImportProcessor.Result result = processor.process(userId, execution, account, statement);

        assertThat(result.detectedRows()).isEqualTo(2);
        assertThat(result.importedRows()).isEqualTo(1);
        assertThat(result.duplicateRows()).isEqualTo(1);
        verify(rawRepository, times(1)).save(any(RawTransaction.class));
        ArgumentCaptor<FinancialTransaction> transaction = ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("40.00");
        assertThat(transaction.getValue().getDirection().name()).isEqualTo("CREDIT");
        assertThat(transaction.getValue().getPostingStatus()).isEqualTo(TransactionPostingStatus.POSTED);
        verify(execution).markParsing();
        verify(execution).complete(2, 1, 1, 0);
    }

    private InterAccountStatement parse(String path) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Recurso ausente: " + path);
            return new InterAccountCsvParser().parse(input);
        }
    }
}
