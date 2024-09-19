package com.alok.home.controller;

import com.alok.home.response.GenericResponse;
import com.alok.home.service.GoogleSheetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/gsheet")
public class GoogleSheetController {

    private GoogleSheetService googleSheetService;

    private ExecutorService virtualThreadExecutorService;
    private static final int REFRESH_CASH_CONTROL = 120;

    public GoogleSheetController(GoogleSheetService googleSheetService, ExecutorService virtualThreadExecutorService) {
        this.googleSheetService = googleSheetService;
        this.virtualThreadExecutorService = virtualThreadExecutorService;
    }

    @GetMapping("/refresh/tax")
    public ResponseEntity<GenericResponse> refreshTaxData() throws IOException {

//        CompletableFuture.runAsync(() -> {
//            try {
//                log.info(Thread.currentThread().toString());
//                googleSheetService.refreshTaxData();
//                googleSheetService.refreshTaxMonthlyData();
//            } catch (IOException |RuntimeException e) {
//                log.error("Google Sheet refresh failed with error: " + e.getMessage());
//                e.printStackTrace();
//                throw new RuntimeException(e);
//            }
//        }, virtualThreadExecutorService);

        log.info(Thread.currentThread().toString());
        googleSheetService.refreshTaxData();
        googleSheetService.refreshTaxMonthlyData();

        log.info(Thread.currentThread().toString());
        return ResponseEntity.accepted()
                .cacheControl(CacheControl.maxAge(REFRESH_CASH_CONTROL, TimeUnit.SECONDS).noTransform().mustRevalidate())
                .body(GenericResponse.builder()
                        .status(GenericResponse.Status.SUCCESS)
                        .message("Refresh Submitted")
                        .build());
    }

    @GetMapping("/refresh/expense")
    public ResponseEntity<GenericResponse> refreshExpenseData() throws IOException {

//        CompletableFuture.runAsync(() -> {
//            try {
//                googleSheetService.refreshExpenseData();
//            } catch (IOException |RuntimeException e) {
//                log.error("Google Sheet refresh failed with error: " + e.getMessage());
//                e.printStackTrace();
//                throw new RuntimeException(e);
//            }
//        }, virtualThreadExecutorService);
        googleSheetService.refreshExpenseData();

        return ResponseEntity.accepted()
                .cacheControl(CacheControl.maxAge(REFRESH_CASH_CONTROL, TimeUnit.SECONDS).noTransform().mustRevalidate())
                .body(GenericResponse.builder()
                        .status(GenericResponse.Status.SUCCESS)
                        .message("Refresh Submitted")
                        .build());
    }

    @GetMapping(value = "/refresh/expense", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> refreshExpenseDataStream() throws IOException {

        return googleSheetService.refreshExpenseDataStream();

//        return ResponseEntity.accepted()
//                .cacheControl(CacheControl.maxAge(REFRESH_CASH_CONTROL, TimeUnit.SECONDS).noTransform().mustRevalidate())
//                .body("Refresh submitted");

    }

    @GetMapping("/refresh/investment")
    public ResponseEntity<GenericResponse> refreshInvestmentData() {

        CompletableFuture.runAsync(() -> {
            try {
                googleSheetService.refreshInvestmentData();
            } catch (IOException |RuntimeException e) {
                log.error("Google Sheet refresh failed with error: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });

        return ResponseEntity.accepted()
                .cacheControl(CacheControl.maxAge(REFRESH_CASH_CONTROL, TimeUnit.SECONDS).noTransform().mustRevalidate())
                .body(GenericResponse.builder()
                        .status(GenericResponse.Status.SUCCESS)
                        .message("Refresh Submitted")
                        .build());
    }

    @GetMapping("/refresh/odion/transactions")
    public ResponseEntity<GenericResponse> refreshOdionTransactions() {

        CompletableFuture.runAsync(() -> {
            try {
                googleSheetService.refreshOdionTransactionsData();
            } catch (IOException |RuntimeException e) {
                log.error("Google Sheet refresh failed with error: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });

        return ResponseEntity.accepted()
                .cacheControl(CacheControl.maxAge(REFRESH_CASH_CONTROL, TimeUnit.SECONDS).noTransform().mustRevalidate())
                .body(GenericResponse.builder()
                        .status(GenericResponse.Status.SUCCESS)
                        .message("Refresh Submitted")
                        .build());
    }
}