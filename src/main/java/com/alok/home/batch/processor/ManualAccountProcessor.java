package com.alok.home.batch.processor;

import com.alok.home.batch.utils.Utility;
import com.alok.home.commons.constant.MDCKey;
import com.alok.home.commons.entity.RawTransaction;
import com.alok.home.commons.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component("manualAccountProcessor")
@Slf4j
public class ManualAccountProcessor implements ItemProcessor<Transaction, Transaction> {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMMyy");
    int dateFiledLength = 7;

    public void setDateFiledLength(int dateFiledLength) {
        this.dateFiledLength = dateFiledLength;
    }

    public void setSimpleDateFormat(SimpleDateFormat simpleDateFormat) {
        this.simpleDateFormat = simpleDateFormat;
    }

    @Override
    public Transaction process(Transaction transaction) throws ParseException {
        return transaction;
    }
}
