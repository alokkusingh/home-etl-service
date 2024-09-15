package com.alok.home.batch.processor;

import com.alok.home.commons.entity.Tax;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component("defaultTaxProcessor")
@Slf4j
public class DefaultTaxProcessor implements ItemProcessor<Tax, Tax> {

    @Override
    public Tax process(Tax taxRecord) throws ParseException {
        return taxRecord;
    }

}
