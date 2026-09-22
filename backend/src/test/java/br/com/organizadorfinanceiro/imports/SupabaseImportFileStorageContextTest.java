package br.com.organizadorfinanceiro.imports;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class SupabaseImportFileStorageContextTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ImportStorageProperties.class, SupabaseImportFileStorageContextTest::properties)
            .withUserConfiguration(StorageConfiguration.class);

    @Test
    void createsStorageWithTheProductionConstructor() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SupabaseImportFileStorage.class);
        });
    }

    private static ImportStorageProperties properties() {
        ImportStorageProperties properties = new ImportStorageProperties();
        properties.setSupabaseUrl("https://example.supabase.co");
        return properties;
    }

    @Configuration(proxyBeanMethods = false)
    @Import(SupabaseImportFileStorage.class)
    static class StorageConfiguration {
    }
}
