package com.alok.home.service.gsheet;

import com.alok.home.commons.entity.Expense;
import com.alok.home.commons.repository.ExpenseRepository;
import com.alok.home.grpc.ExpenseCategorizerClient;
import com.alok.home.service.GoogleSheetService;
import com.google.api.services.sheets.v4.Sheets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class ExpenseSheetService extends GoogleSheetService {
    private final Sheets sheets;

    private final ExpenseRepository expenseRepository;

    private final String sheetId;
    private final String expenseSheetRange;

    private final ExpenseCategorizerClient expenseCategorizerClient;

    private final SimpleDateFormat simpleDateFormat;

    private final ExecutorService virtualThreadExecutorService;

    public ExpenseSheetService(
            Sheets sheets,
            @Value("${sheet.id.expense}") String sheetId,
            @Value("${range.expense-sheet}") String expenseSheetRange,
            ExpenseRepository expenseRepository,
            ExpenseCategorizerClient expenseCategorizerClient, ExecutorService virtualThreadExecutorService
    ) {
        super();
        this.expenseRepository = expenseRepository;
        this.expenseSheetRange = expenseSheetRange;
        this.expenseCategorizerClient = expenseCategorizerClient;
        this.sheets = sheets;
        this.sheetId = sheetId;
        this.virtualThreadExecutorService = virtualThreadExecutorService;
        this.simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
    }

    @Override
    public void refreshSheet() throws IOException {
        CompletableFuture.supplyAsync(() -> {
                    try {
                        log.info(Thread.currentThread().toString());
                        log.info("Fetching the records");
                        return sheets.spreadsheets().values()
                                .get(sheetId, expenseSheetRange)
                                .execute();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, virtualThreadExecutorService)
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                })
                .thenApply(
                        response -> {
                            log.info(Thread.currentThread().toString());
                            log.info("Transforming the records");
                            return Optional.ofNullable(response.getValues()).orElse(Collections.emptyList()).stream()
                                    .filter(row -> row.get(2) != null && ((String) row.get(2)).length() != 0)
                                    .map(row -> Expense.builder()
                                            .date(parseToDate((String) row.get(0)))
                                            .head((String) row.get(1))
                                            .amount(Double.parseDouble((String) row.get(2)))
                                            .comment(row.get(3) == null ? "" : truncateString((String) row.get(3), 250))
                                            .yearx(row.get(4) == null ? 0 : Integer.parseInt((String) row.get(4)))
                                            .monthx(row.get(5) == null ? 0 : Integer.parseInt((String) row.get(5)))
                                            //.category(Utility.getExpenseCategory((String) row.get(1), row.get(3) == null? "": (String) row.get(3)))
                                            .category(row.get(1) == null ? "" : expenseCategorizerClient.getExpenseCategory((String) row.get(1)))
                                            .build()
                                    )
                                    .toList();
                        }
                )
                .thenApply(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Number of transactions: {}", records.size());
                    return records;
                })
                .thenApply(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Deleting the records");
                    expenseRepository.deleteAll();
                    return records;
                })
                .thenAccept(records -> {
                    log.info("Inserting the records");
                    expenseRepository.saveAll(records);
                })
                .thenAccept(records -> {
                    log.info("Refresh Completed");
                });
    }

    @Override
    public Flux<String> refreshSheetStream() throws IOException {
        expenseRepository.deleteAll();

        AtomicInteger count = new AtomicInteger();

        return Flux.fromIterable(
                        Optional.ofNullable(sheets.spreadsheets().values().get(sheetId, expenseSheetRange).execute().getValues())
                                .orElse(Collections.emptyList())
                )
                .filter(row -> row.get(2) != null && !((String) row.get(2)).isEmpty())
                .map(row -> Expense.builder()
                        .date(parseToDate((String) row.get(0)))
                        .head((String) row.get(1))
                        .amount(Double.parseDouble((String) row.get(2)))
                        .comment(row.get(3) == null ? "" : (String) row.get(3))
                        .yearx(row.get(4) == null ? 0 : Integer.parseInt((String) row.get(4)))
                        .monthx(row.get(5) == null ? 0 : Integer.parseInt((String) row.get(5)))
                        .category(row.get(1) == null ? "" : expenseCategorizerClient.getExpenseCategory((String) row.get(1)))
                        .build()
                )
                .map(expense -> {
                    expenseRepository.save(expense);
                    log.info(count.toString());
                    return count.incrementAndGet();
                })
                .map(Object::toString);
//                .subscribe(
//                        expense -> {
//                            log.info(expense.getCategory());
//                            count.incrementAndGet();
//                            expenseRepository.save(expense);
//                        }
//                );

    }

    private Date parseToDate(String strDate) {
        try {
            return simpleDateFormat.parse(strDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private String truncateString(String text, int length) {
        if (text.length() <= length) {
            return text;
        } else {
            return text.substring(0, length);
        }
    }
}
