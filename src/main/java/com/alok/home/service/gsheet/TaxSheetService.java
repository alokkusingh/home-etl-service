package com.alok.home.service.gsheet;

import com.alok.home.commons.entity.Tax;
import com.alok.home.commons.entity.TaxMonthly;
import com.alok.home.commons.repository.*;
import com.alok.home.service.GoogleSheetService;
import com.google.api.services.sheets.v4.Sheets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class TaxSheetService extends GoogleSheetService {
    private final Sheets sheets;

    private final TaxRepository taxRepository;
    private final TaxMonthlyRepository taxMonthlyRepository;

    private final String sheetId;
    private final String taxSheetRange;
    private final String taxMonthSheetRange;

    private final ExecutorService virtualThreadExecutorService;


    public TaxSheetService(
            Sheets sheets,
            @Value("${sheet.id.expense}") String sheetId,
            @Value("${range.tax-sheet}") String taxSheetRange,
            @Value("${range.tax-sheet-monthly}") String taxMonthSheetRange,
            TaxRepository taxRepository,
            TaxMonthlyRepository taxMonthlyRepository,
            ExecutorService virtualThreadExecutorService
    ) {
        super();
        this.virtualThreadExecutorService = virtualThreadExecutorService;
        this.sheets = sheets;
        this.taxRepository = taxRepository;
        this.taxMonthlyRepository = taxMonthlyRepository;
        this.sheetId = sheetId;
        this.taxSheetRange = taxSheetRange;
        this.taxMonthSheetRange = taxMonthSheetRange;
    }

    @Override
    public void refreshSheet() throws IOException {
        refreshTaxData();
        refreshTaxMonthlyData();
    }

    public void refreshTaxData() throws IOException {

        CompletableFuture.supplyAsync(() -> {
                    try {
                        log.info(Thread.currentThread().toString());
                        log.info("Fetching the Tax records");
                        return sheets.spreadsheets().values().get(sheetId, taxSheetRange).execute();
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
                            log.info("Transforming the Tax records");
                            return Optional.ofNullable(response.getValues()).orElse(Collections.emptyList()).stream()
                                    .map(row -> Tax.builder()
                                            .financialYear((String) row.get(0))
                                            .paidAmount(row.size() < 2 ? 0 : Integer.parseInt((String) row.get(1)))
                                            .refundAmount(row.size() < 3 ? 0 : Integer.parseInt((String) row.get(2)))
                                            .build()
                                    )
                                    .toList();
                        }
                )
                .thenApply(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Number of Tax transactions: {}", records.size());
                    return records;
                })
                .thenApply(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Deleting the Tax records");
                    taxRepository.deleteAll();
                    return records;
                })
                .thenApply(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Inserting the Tax records");
                    taxRepository.saveAll(records);
                    return records;
                }).thenAccept(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Inserted Number of Tax records: {}", records.size());
                });
        //.join();
    }

    private void refreshTaxMonthlyData() throws IOException {
        CompletableFuture.supplyAsync(() -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Fetching the Monthly Tax records");
                    try {
                        return sheets.spreadsheets().values()
                                .get(sheetId, taxMonthSheetRange)
                                .execute();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, virtualThreadExecutorService)
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                })
                .thenApply(response -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Transforming Monthly Tax the records");
                    return Optional.ofNullable(response.getValues()).orElse(Collections.emptyList()).stream()
                            .filter(row -> row.size() > 1)
                            .map(row -> TaxMonthly.builder()
                                    .yearx((short) YearMonth.parse((String) row.get(0)).getYear())
                                    .monthx((short) YearMonth.parse((String) row.get(0)).getMonth().getValue())
                                    .paidAmount(Integer.parseInt((String) row.get(1)))
                                    .build()
                            )
                            .toList();
                })
                .thenApply(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Number of Monthly Tax transactions: {}", records.size());
                    return records;
                })
                .thenApply(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Deleting the Monthly Tax records");
                    taxMonthlyRepository.deleteAll();
                    return records;
                })
                .thenApply(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Inserting the  Monthly Tax records");
                    taxMonthlyRepository.saveAll(records);
                    return records;
                })
                .thenAccept(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Inserted Number of Monthly Tax records: {}", records.size());
                });
    }
}
