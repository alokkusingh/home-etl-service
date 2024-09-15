package com.alok.home.batch.writer;

import com.alok.home.commons.entity.Expense;
import com.alok.home.commons.repository.ExpenseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExpenseDbWriter implements ItemWriter<Expense> {

    @Autowired
    ExpenseRepository expenseRepository;

    @Override
    public void write(Chunk<? extends Expense> records) throws Exception {
       /* records.stream()
        .sorted()
        .filter(Transaction::isSalary)
        .forEach(
        record -> log.debug("Parsed record: {}", record )
        );*/
        expenseRepository.saveAll(records);
    }
}
