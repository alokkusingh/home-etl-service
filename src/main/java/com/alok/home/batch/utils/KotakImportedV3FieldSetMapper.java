package com.alok.home.batch.utils;

import com.alok.home.commons.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

@Slf4j
public class KotakImportedV3FieldSetMapper implements FieldSetMapper<Transaction> {

    @Override
    public Transaction mapFieldSet(FieldSet fieldSet) {
        String slNo = fieldSet.readString("slNo");
        if (slNo == null || slNo.isBlank() || !slNo.trim().matches("\\d+")) {
            log.debug("Skipping non-transaction Kotak V3 row. slNo={}", slNo);
            throw new FlatFileParseException("Skipping metadata row", "", 0);
        }

        log.debug("Mapping fieldSet to Transaction, SrlNo: {}", slNo);
        Transaction transaction = new Transaction();
        String strDate = fieldSet.readString("date");
        transaction.setDate(fieldSet.readDate("date", Utility.getDateFormat(strDate)));
        transaction.setDescription(fieldSet.readString("description"));
        String drCr = fieldSet.readString("drCr");

        String debitAmount = fieldSet.readString("debitAmount")
                .replace(",", "")
                .replace("-", "");
        String creditAmount = fieldSet.readString("creditAmount")
                .replace(",", "")
                .replace("-", "");

        if (!debitAmount.isEmpty()) {
            transaction.setCredit((int) Math.round(Double.parseDouble(debitAmount)));
            transaction.setDebit(0);
        } else {
            transaction.setDebit((int) Math.round(Double.parseDouble(creditAmount)));
            transaction.setCredit(0);
        }

        log.debug("Mapped transaction {}", transaction);

        return transaction;
    }
}
