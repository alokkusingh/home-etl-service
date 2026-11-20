package com.alok.home.service.gsheet;

import com.alok.home.commons.entity.Expense;
import com.alok.home.commons.repository.ExpenseRepository;
import com.alok.home.grpc.ExpenseCategorizerClient;
import com.alok.home.grpc.stub.ExpenseCategorizerOuterClass;
import com.alok.home.service.GoogleSheetService;
import com.google.api.services.sheets.v4.Sheets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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
            @Value("${sheet.range.expense-sheet}") String expenseSheetRange,
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
        CompletableFuture.supplyAsync(this::fetchSheetRows, virtualThreadExecutorService)
                .thenApply(this::mapRowsToExpenses)
                .thenApply(records -> {
                    log.info("Number of transactions: {}", records.size());
                    log.info("Deleting the records");
                    expenseRepository.deleteAll();
                    return records;
                })
                .thenAccept(records -> {
                    log.info("Inserting the records");
                    expenseRepository.saveAll(records);
                    log.info("Refresh Completed");
                })
                .exceptionally(ex -> {
                    log.error("Refresh failed; aborted before modifying database", ex);
                    return null;
                });
    }

    @Override
    public Flux<String> refreshSheetStream() {
        AtomicInteger count = new AtomicInteger();

        return Flux.defer(() -> {
            List<List<Object>> rows = fetchSheetRows();
            List<Expense> expenses = mapRowsToExpenses(rows);

            expenseRepository.deleteAll();

            return Flux.fromIterable(expenses)
                    .buffer(200)
                    .map(batch -> {
                        expenseRepository.saveAll(batch);
                        int processed = count.addAndGet(batch.size());
                        log.info("Saved {} / {} records", processed, expenses.size());
                        return String.valueOf(processed);
                    });
        });
    }

    private List<List<Object>> fetchSheetRows() {
        try {
            log.info("Fetching records from Google Sheets on {}", Thread.currentThread());
            var response = sheets.spreadsheets().values()
                    .get(sheetId, expenseSheetRange)
                    .execute();
            return Optional.ofNullable(response.getValues()).orElse(Collections.emptyList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch Google Sheet", e);
        }
    }

    private List<Expense> mapRowsToExpenses(List<List<Object>> rawRows) {
        log.info("Transforming records with 3-parameter ML categorization");

        List<List<Object>> validRows = rawRows.stream()
                .filter(row -> !getCell(row, 2).isEmpty())
                .toList();

        List<ExpenseCategorizerOuterClass.ExpenseCategorizationRequest> grpcRequests = validRows.stream()
                .map(row -> ExpenseCategorizerOuterClass.ExpenseCategorizationRequest.newBuilder()
                        .setHead(getCell(row, 1))
                        .setComment(getCell(row, 3)) // Full untruncated comment for ML
                        .setAmount(Double.parseDouble(getCell(row, 2)))
                        .build())
                .toList();

        List<ExpenseCategorizerClient.PredictionResult> predictions =
                expenseCategorizerClient.predictExpenseCategoriesBatch(grpcRequests);

        List<Expense> expenses = new ArrayList<>(validRows.size());
        for (int i = 0; i < validRows.size(); i++) {
            List<Object> row = validRows.get(i);
            String head = getCell(row, 1);
            double amount = Double.parseDouble(getCell(row, 2));
            String rawComment = getCell(row, 3);
            String yearStr = getCell(row, 4);
            String monthStr = getCell(row, 5);

            String category = head.isEmpty() ? "" : predictions.get(i).category();

            expenses.add(Expense.builder()
                    .date(parseToDate(getCell(row, 0)))
                    .head(head)
                    .amount(amount)
                    .comment(rawComment.isEmpty() ? "" : truncateString(rawComment, 250))
                    .yearx(yearStr.isEmpty() ? 0 : Integer.parseInt(yearStr))
                    .monthx(monthStr.isEmpty() ? 0 : Integer.parseInt(monthStr))
                    .category(category)
                    .build());
        }
        return expenses;
    }

    private String getCell(List<Object> row, int index) {
        if (index >= row.size() || row.get(index) == null) {
            return "";
        }
        return row.get(index).toString().trim();
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
