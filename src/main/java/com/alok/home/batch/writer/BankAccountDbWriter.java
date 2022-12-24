package com.alok.home.batch.writer;

import com.alok.home.commons.model.Transaction;
import com.alok.home.commons.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BankAccountDbWriter implements ItemWriter<Transaction> {

    @Autowired
    TransactionRepository transactionRepository;

    @Override
    public void write(Chunk<? extends Transaction> chunk) throws Exception {
       /* records.stream()
                .sorted()
                .filter(Transaction::isSalary)
                .forEach(
                record -> log.debug("Parsed record: {}", record )
        );*/

        transactionRepository.saveAll(chunk.getItems());
    }
}
