package com.alok.home.batch.writer;

import com.alok.home.commons.entity.Tax;
import com.alok.home.commons.repository.TaxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TaxDbWriter implements ItemWriter<Tax> {

    @Autowired
    TaxRepository taxRepository;

    @Override
    public void write(Chunk<? extends Tax> chunk) throws Exception {
        taxRepository.saveAll(chunk.getItems()) ;
    }
}
