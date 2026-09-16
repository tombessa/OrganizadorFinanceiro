package br.com.organizadorfinanceiro.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class StoragePathFactoryTest {
    private static final UUID USER_ID = UUID.fromString("8fa7ae07-a77c-4801-9a8b-49f15eed192a");
    private static final String HASH = "a".repeat(64);

    @Test
    void createsTenantScopedPathWithoutOriginalFilename() {
        String path = StoragePathFactory.create(USER_ID, SourceAdapter.INTER_ACCOUNT_CSV, HASH,
                "Extrato com dados pessoais.csv");

        assertThat(path).isEqualTo(USER_ID + "/inter_account_csv/" + HASH + ".csv");
        assertThat(path).doesNotContain("Extrato", "dados", "pessoais");
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThatThrownBy(() -> StoragePathFactory.create(USER_ID, SourceAdapter.PAYROLL_PDF, HASH, "folha.exe"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
