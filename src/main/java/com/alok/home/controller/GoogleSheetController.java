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
    private final GoogleSheetService taxSheetService;
    private final GoogleSheetService expenseSheetService;
    private final GoogleSheetService investmentSheetService;
    private final GoogleSheetService estateSheetService;
    private final GoogleSheetService timelineSheetService;
    private final GoogleSheetService medicalSheetService;

    private static final int REFRESH_CASH_CONTROL = 120;

    public GoogleSheetController(
            GoogleSheetService taxSheetService,
            GoogleSheetService expenseSheetService,
            GoogleSheetService investmentSheetService,
            GoogleSheetService estateSheetService,
            GoogleSheetService timelineSheetService,
            GoogleSheetService medicalSheetService
    ) {
        this.taxSheetService = taxSheetService;
        this.expenseSheetService = expenseSheetService;
        this.investmentSheetService = investmentSheetService;
        this.estateSheetService = estateSheetService;
        this.timelineSheetService = timelineSheetService;
        this.medicalSheetService = medicalSheetService;
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
        taxSheetService.refreshSheet();

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
        expenseSheetService.refreshSheet();

        return ResponseEntity.accepted()
                .cacheControl(CacheControl.maxAge(REFRESH_CASH_CONTROL, TimeUnit.SECONDS).noTransform().mustRevalidate())
                .body(GenericResponse.builder()
                        .status(GenericResponse.Status.SUCCESS)
                        .message("Refresh Submitted")
                        .build());
    }

    @GetMapping(value = "/refresh/expense", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> refreshExpenseDataStream() throws IOException {

        return expenseSheetService.refreshSheetStream();

//        return ResponseEntity.accepted()
//                .cacheControl(CacheControl.maxAge(REFRESH_CASH_CONTROL, TimeUnit.SECONDS).noTransform().mustRevalidate())
//                .body("Refresh submitted");

    }

    @GetMapping("/refresh/investment")
    public ResponseEntity<GenericResponse> refreshInvestmentData() {

        CompletableFuture.runAsync(() -> {
            try {
                investmentSheetService.refreshSheet();
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
                estateSheetService.refreshSheet();
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

    @GetMapping("/refresh/events/life")
    public ResponseEntity<GenericResponse> refreshLifeEvents() {

        CompletableFuture.runAsync(() -> {
            try {
                timelineSheetService.refreshSheet();
            } catch (IOException |RuntimeException e) {
                log.error("Google Sheet refresh failed with error: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
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

    @GetMapping("/refresh/medical")
    public ResponseEntity<GenericResponse> refreshMedicalReport() {

        CompletableFuture.runAsync(() -> {
            try {
                medicalSheetService.refreshSheet();
            } catch (IOException |RuntimeException e) {
                log.error("Google Sheet refresh failed with error: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
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