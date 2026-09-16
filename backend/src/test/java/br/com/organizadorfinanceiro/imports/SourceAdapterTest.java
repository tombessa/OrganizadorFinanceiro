package br.com.organizadorfinanceiro.imports;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SourceAdapterTest {
    @Test
    void exposesTheRequiredTargetAndVersion() {
        assertThat(SourceAdapter.INTER_ACCOUNT_CSV.targetType()).isEqualTo(ImportTargetType.ACCOUNT);
        assertThat(SourceAdapter.INTER_CARD_CSV.targetType()).isEqualTo(ImportTargetType.CARD);
        assertThat(SourceAdapter.PAYROLL_PDF.targetType()).isEqualTo(ImportTargetType.PAYROLL);
        assertThat(SourceAdapter.values()).allSatisfy(adapter -> assertThat(adapter.version()).isNotBlank());
    }
}
