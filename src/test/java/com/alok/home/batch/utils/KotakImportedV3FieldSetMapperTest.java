package com.alok.home.batch.utils;

import com.alok.home.batch.mapper.KotakImportedV3FieldSetMapper;
import com.alok.home.commons.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class KotakImportedV3FieldSetMapperTest {

    private final KotakImportedV3FieldSetMapper mapper = new KotakImportedV3FieldSetMapper();

    @Test
    void shouldSkipRowWhenDateIsBlank() {
        DefaultFieldSet fieldSet = new DefaultFieldSet(
                new String[]{"", "Coffee", "DR", "", "100.00", "7"},
                new String[]{"date", "description", "drCr", "debitAmount", "creditAmount", "slNo"}
        );

        assertNull(mapper.mapFieldSet(fieldSet));
    }

    @Test
    void shouldMapValidRowWithTimestamp() {
        DefaultFieldSet fieldSet = new DefaultFieldSet(
                new String[]{"01-10-2026 02:23:27", "Coffee", "DR", "100.00", "", "7"},
                new String[]{"date", "description", "drCr", "debitAmount", "creditAmount", "slNo"}
        );

        Transaction transaction = mapper.mapFieldSet(fieldSet);

        assertNotNull(transaction);
        assertNotNull(transaction.getDate());
        assertNotNull(transaction.getDescription());
    }
}
