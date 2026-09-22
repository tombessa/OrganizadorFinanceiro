package br.com.organizadorfinanceiro.imports;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class SupabaseImportFileStorageContextTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("app.import-storage.supabase-url=https://example.supabase.co")
            .withUserConfiguration(StorageConfiguration.class);

    @Test
    void createsStorageWithTheProductionConstructor() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SupabaseImportFileStorage.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({ImportStorageProperties.class, SupabaseImportFileStorage.class})
    static class StorageConfiguration {
    }
}
