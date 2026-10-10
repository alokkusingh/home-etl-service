package com.alok.home.batch.reader;

import com.alok.home.batch.utils.KotakImportedV3FieldSetMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;

class CSVReaderTest {

    @Test
    void kotakV3SkipsStatementMetadataRows() {
        String[] fieldNames = {"slNo", "trDate", "date", "description", "chqRefNumber", "debitAmount", "creditAmount", "closingBalance", "drCr"};
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setQuoteCharacter('"');
        tokenizer.setNames(fieldNames);

        FieldSet metadata = tokenizer.tokenize("\"\"\",\"\"\",\"Account Statement\"");
        Assertions.assertThrows(org.springframework.batch.item.file.FlatFileParseException.class,
                () -> new KotakImportedV3FieldSetMapper().mapFieldSet(metadata));

        FieldSet transaction = tokenizer.tokenize("\"2\",\"29-09-2026 22:09:28\",\"29-09-2026\",\"SWEEP TRANSFER TO [0517349142]\",\"\",\"35,000.00\",\"\",\"54,271.07\",\"CR\"");
        Assertions.assertNotNull(new KotakImportedV3FieldSetMapper().mapFieldSet(transaction));
    }
}
