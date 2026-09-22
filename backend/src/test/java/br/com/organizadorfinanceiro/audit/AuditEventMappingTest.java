package br.com.organizadorfinanceiro.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;

class AuditEventMappingTest {
    @Test
    void mapsAuditEventsAsAppendOnlyEvenWithMonetaryPayloads() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("overdraftLimit", new BigDecimal("1000.00"));

        AuditEvent event = new AuditEvent(
                UUID.randomUUID(), "FINANCIAL_ACCOUNT", UUID.randomUUID(), "CREATED", payload);
        payload.put("overdraftLimit", new BigDecimal("2000.00"));

        assertThat(AuditEvent.class.isAnnotationPresent(Immutable.class)).isTrue();
        assertThat(persistentFields()).allSatisfy(field -> {
            Column column = field.getAnnotation(Column.class);
            assertThat(column.updatable())
                    .as("Audit field %s must not participate in SQL UPDATE", field.getName())
                    .isFalse();
        });
        assertThat(afterValue(event).get("overdraftLimit")).isEqualTo(new BigDecimal("1000.00"));
    }

    private Field[] persistentFields() {
        return java.util.Arrays.stream(AuditEvent.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .toArray(Field[]::new);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> afterValue(AuditEvent event) throws Exception {
        Field field = AuditEvent.class.getDeclaredField("afterValue");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(event);
    }
}
