package com.alok.home.batch.utils;

import com.alok.home.commons.entity.Transaction;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

public class BankUtils {

    public enum LineMapperType {
        KOTAK,
        HDFC,
        KOTAK_V3
    }

    public static LineMapper<Transaction> importedAccountLineMapper(String[] fieldNames, LineMapperType lineMapperType) {
        DefaultLineMapper<Transaction> defaultLineMapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames(fieldNames);

        var fieldSetMapper = switch (lineMapperType) {
            case KOTAK -> new KotakImportedFieldSetMapper();
            case HDFC -> new HDFCImportedFieldSetMapper();
            case KOTAK_V3 -> new KotakImportedV3FieldSetMapper();
        };

//        if (lineMapperType == LineMapperType.KOTAK)
//            fieldSetMapper = new KotakImportedFieldSetMapper();
//        else if (lineMapperType == LineMapperType.HDFC)
//            fieldSetMapper = new HDFCImportedFieldSetMapper();
//        else if (lineMapperType == LineMapperType.KOTAK_V3) {
//            fieldSetMapper = new KotakImportedV3FieldSetMapper();
//        }

        defaultLineMapper.setLineTokenizer(lineTokenizer);
        defaultLineMapper.setFieldSetMapper(fieldSetMapper);

        return defaultLineMapper;
    }
}
