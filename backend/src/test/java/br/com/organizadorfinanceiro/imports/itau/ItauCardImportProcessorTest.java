package br.com.organizadorfinanceiro.imports.itau;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.com.organizadorfinanceiro.cards.CreditCard;
import br.com.organizadorfinanceiro.imports.ImportDocumentStatus;
import br.com.organizadorfinanceiro.imports.ImportExecution;
import br.com.organizadorfinanceiro.imports.SourceAdapter;
import br.com.organizadorfinanceiro.shared.HashingService;
import br.com.organizadorfinanceiro.transactions.FinancialTransaction;
import br.com.organizadorfinanceiro.transactions.FinancialTransactionRepository;
import br.com.organizadorfinanceiro.transactions.RawTransaction;
import br.com.organizadorfinanceiro.transactions.RawTransactionRepository;
import br.com.organizadorfinanceiro.transactions.TransactionPostingStatus;
import br.com.organizadorfinanceiro.transactions.TransactionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ItauCardImportProcessorTest {
    @Test
    void importsPaymentAsNeutralStatementPaymentAndFuturePurchaseAsProjected() {
        UUID userId = UUID.fromString("6d5d4797-3df9-41ed-b5ca-0d9e606bc44e");
        CreditCard card = mock(CreditCard.class);
        ImportExecution execution = mock(ImportExecution.class);
        RawTransactionRepository rawRepository = mock(RawTransactionRepository.class);
        FinancialTransactionRepository transactionRepository = mock(FinancialTransactionRepository.class);
        when(card.getId()).thenReturn(UUID.fromString("cf4b6372-9df5-4ea5-a9d8-27c7869498d8"));
        when(card.getLastFourDigits()).thenReturn("5767");
        when(rawRepository.findExistingFingerprints(eq(userId), anyCollection())).thenReturn(Set.of());
        when(rawRepository.save(any(RawTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ItauCardStatement statement = statement(ImportDocumentStatus.PROJECTED, List.of(
                entry(15, "Pagamento Boleto", "-100.00", true),
                entry(16, "Compra sintética", "80.00", false)));

        ItauCardImportProcessor processor = new ItauCardImportProcessor(rawRepository, transactionRepository,
                new ItauCardFingerprint(new HashingService()));
        ItauCardImportProcessor.Result result = processor.process(userId, execution, card, statement);

        assertThat(result.importedRows()).isEqualTo(2);
        ArgumentCaptor<FinancialTransaction> transactions = ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(transactions.capture());
        FinancialTransaction payment = transactions.getAllValues().getFirst();
        assertThat(payment.getSource()).isEqualTo(SourceAdapter.ITAU_CARD_XLSX);
        assertThat(payment.getTransactionType()).isEqualTo(TransactionType.CREDIT_CARD_PAYMENT);
        assertThat(payment.isStatementPayment()).isTrue();
        assertThat(payment.getPostingStatus()).isEqualTo(TransactionPostingStatus.PROJECTED);
        verify(execution).complete(2, 2, 0, 0);
    }

    @Test
    void rejectsAStatementAssignedToTheWrongMasterCard() {
        CreditCard card = mock(CreditCard.class);
        when(card.getLastFourDigits()).thenReturn("9999");
        ItauCardImportProcessor processor = new ItauCardImportProcessor(
                mock(RawTransactionRepository.class), mock(FinancialTransactionRepository.class),
                new ItauCardFingerprint(new HashingService()));

        assertThatThrownBy(() -> processor.process(UUID.randomUUID(), mock(ImportExecution.class), card,
                statement(ImportDocumentStatus.OPEN, List.of(entry(15, "Compra", "80.00", false)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("final 5767");
    }

    private ItauCardStatement statement(ImportDocumentStatus status, List<ItauCardStatement.Entry> entries) {
        return new ItauCardStatement(status, YearMonth.of(2099, 9), LocalDate.of(2099, 9, 10),
                new BigDecimal("80.00"), "5767", entries);
    }

    private ItauCardStatement.Entry entry(int row, String description, String amount, boolean payment) {
        return new ItauCardStatement.Entry(row, LocalDate.of(2099, 8, 20), description,
                new BigDecimal(amount), null, null, "5767", "Físico", payment);
    }
}
