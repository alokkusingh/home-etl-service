package com.alok.home.service;

import com.alok.home.commons.constant.InvestmentType;
import com.alok.home.commons.entity.*;
import com.alok.home.commons.repository.*;
import com.alok.home.grpc.ExpenseCategorizerClient;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class GoogleSheetService {

    private String serviceAccountKeyFile;
    private String expenseSheetId;
    private String taxSheetRange;
    private String taxMonthSheetRange;
    private String expenseSheetRange;
    private String investmentSheetRange;
    private Sheets sheetsService;
    private String odionSheetId;
    private String odionTransactionsSheetRange;
    private ExpenseRepository expenseRepository;
    private TaxRepository taxRepository;
    private TaxMonthlyRepository taxMonthlyRepository;
    private InvestmentRepository investmentRepository;
    private OdionTransactionRepository odionTransactionRepository;

    private ExpenseCategorizerClient expenseCategorizerClient;

    private final SimpleDateFormat simpleDateFormat;

    private ExecutorService virtualThreadExecutorService;

    public GoogleSheetService(
            @Value("${file.path.service_account.key}") String serviceAccountKeyFile,
            @Value("${sheet.id.expense}") String expenseSheetId,
            @Value("${range.tax-sheet}") String taxSheetRange,
            @Value("${range.tax-sheet-monthly}") String taxMonthSheetRange,
            @Value("${range.expense-sheet}") String expenseSheetRange,
            @Value("${range.investment-sheet}") String investmentSheetRange,
            @Value("${sheet.id.odion}") String odionSheetId,
            @Value("${range.odion.transaction}") String odionTransactionsSheetRange,
            ExpenseRepository expenseRepository,
            TaxRepository taxRepository,
            TaxMonthlyRepository taxMonthlyRepository,
            InvestmentRepository investmentRepository,
            OdionTransactionRepository odionTransactionRepository,
            ExpenseCategorizerClient expenseCategorizerClient,
            ExecutorService virtualThreadExecutorService) {
        this.serviceAccountKeyFile = serviceAccountKeyFile;
        this.expenseSheetId = expenseSheetId;
        this.taxSheetRange = taxSheetRange;
        this.taxMonthSheetRange = taxMonthSheetRange;
        this.expenseSheetRange = expenseSheetRange;
        this.investmentSheetRange = investmentSheetRange;
        this.odionSheetId = odionSheetId;
        this.odionTransactionsSheetRange = odionTransactionsSheetRange;
        this.expenseRepository = expenseRepository;
        this.taxRepository = taxRepository;
        this.taxMonthlyRepository = taxMonthlyRepository;
        this.investmentRepository = investmentRepository;

//        InputStream inputStream = new FileInputStream(serviceAccountKeyFile); // put your service account's key.json file in asset folder.
//
//
//        GoogleCredentials googleCredentials = GoogleCredentials.fromStream(inputStream)
//                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));
//
//        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);
//
//        sheetsService = new Sheets.Builder(
//                GoogleNetHttpTransport.newTrustedTransport(),
//                GsonFactory.getDefaultInstance(),
//                requestInitializer
//        )
//                .setApplicationName("Home Stack")
//                .build();
        this.odionTransactionRepository = odionTransactionRepository;
        this.expenseCategorizerClient = expenseCategorizerClient;
        this.virtualThreadExecutorService = virtualThreadExecutorService;
        this.simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
    }

    private void initSheetService() {
        if (sheetsService == null) {
            log.info("Google Sheet Service Initializing!");

            CompletableFuture.supplyAsync(() -> {
                try {
                    log.info(Thread.currentThread().toString());
                    log.info("Reading credentials file");
                    return new FileInputStream(serviceAccountKeyFile);
                } catch (FileNotFoundException e) {
                    log.error("Google Sheet initialization failed with error: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }, virtualThreadExecutorService)
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    })
                    .thenApply(inputStream -> {
                        log.info(Thread.currentThread().toString());
                        log.info("Creating Http Credentials Adaptor");
                        try {
                            return new HttpCredentialsAdapter(GoogleCredentials.fromStream(inputStream)
                                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY)));
                        } catch (IOException e) {
                            log.error("Google Sheet initialization failed with error: " + e.getMessage());
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    })
                    .thenAccept(requestInitializer -> {
                        log.info(Thread.currentThread().toString());
                        log.info("Creating Sheet Builder");
                        try {
                            sheetsService =  new Sheets.Builder(
                                    GoogleNetHttpTransport.newTrustedTransport(),
                                    //GsonFactory.getDefaultInstance(),
                                    JacksonFactory.getDefaultInstance(),
                                    requestInitializer
                            )
                            .setApplicationName("Home Stack")
                            .build();
                        } catch (GeneralSecurityException | IOException | RuntimeException e) {
                            log.error("Google Sheet initialization failed with error: " + e.getMessage());
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    })
                    .join();
        }
    }

    public void refreshTaxData() throws IOException {

        initSheetService();

        CompletableFuture.supplyAsync(() -> {
                try {
                    log.info(Thread.currentThread().toString());
                    log.info("Fetching the records");
                    return sheetsService.spreadsheets().values().get(expenseSheetId, taxSheetRange).execute();
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
                                    .map(row -> Tax.builder()
                                            .financialYear((String) row.get(0))
                                            .paidAmount(row.size() < 2? 0: Integer.parseInt((String) row.get(1)))
                                            .refundAmount(row.size() < 3? 0: Integer.parseInt((String) row.get(2)))
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
                    taxRepository.deleteAll();
                    return records;
                })
                .thenAccept(records -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Inserting the records");
                    taxRepository.saveAll(records);
                });
                //.join();
    }

    public void refreshTaxMonthlyData() throws IOException {
        initSheetService();
        CompletableFuture.supplyAsync(() -> {
            log.info(Thread.currentThread().toString());
            log.info("Fetching the records");
            try {
                return sheetsService.spreadsheets().values()
                        .get(expenseSheetId, taxMonthSheetRange)
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
            log.info("Transforming the records");
            return Optional.ofNullable(response.getValues()).orElse(Collections.emptyList()).stream()
                    .filter(row -> row.size() > 1)
                    .map(row -> TaxMonthly.builder()
                            .yearx((short) YearMonth.parse((String)row.get(0)).getYear())
                            .monthx((short) YearMonth.parse((String)row.get(0)).getMonth().getValue())
                            .paidAmount(Integer.parseInt((String) row.get(1)))
                            .build()
                    )
                    .toList();
        })
        .thenApply(records -> {
            log.info(Thread.currentThread().toString());
            log.info("Number of transactions: {}", records.size());
            return records;
        })
        .thenApply(records -> {
            log.info(Thread.currentThread().toString());
            log.info("Deleting the records");
            taxMonthlyRepository.deleteAll();
            return records;
        })
        .thenAccept(records -> {
            log.info(Thread.currentThread().toString());
            log.info("Inserting the records");
            taxMonthlyRepository.saveAll(records);
        });
    }

    public void refreshExpenseData() throws IOException {
        initSheetService();

        CompletableFuture.supplyAsync(() -> {
            try {
                log.info(Thread.currentThread().toString());
                log.info("Fetching the records");
                return sheetsService.spreadsheets().values()
                        .get(expenseSheetId, expenseSheetRange)
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
                                    .comment(row.get(3) == null ? "" : (String) row.get(3))
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
        });
    }

    public Flux<String> refreshExpenseDataStream() throws IOException {
        initSheetService();
        expenseRepository.deleteAll();

        AtomicInteger count = new AtomicInteger();

        return Flux.fromIterable(
                    Optional.ofNullable(sheetsService.spreadsheets().values().get(expenseSheetId, expenseSheetRange).execute().getValues())
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

    public void refreshInvestmentData() throws IOException {
        initSheetService();
        ValueRange response = sheetsService.spreadsheets().values()
                .get(expenseSheetId, investmentSheetRange)
                .execute();

        List<Investment> records = Optional.ofNullable(response.getValues()).orElse(Collections.emptyList()).stream()
                .map(row -> List.of(
                                Investment.builder()
                                        .yearx(Short.parseShort((String) row.get(0)))
                                        .monthx(Short.parseShort((String) row.get(1)))
                                        .head(InvestmentType.PF.name())
                                        .contribution(((String) row.get(2)).isEmpty() ? 0 : Integer.parseInt((String) row.get(2)))
                                        .contributionAsOnMonth(((String) row.get(3)).isEmpty() ? 0 : Integer.parseInt((String) row.get(3)))
                                        .valueAsOnMonth(((String) row.get(4)).isEmpty() ? 0 : Integer.parseInt((String) row.get(4)))
                                        .build(),
                                Investment.builder()
                                        .yearx(Short.parseShort((String) row.get(0)))
                                        .monthx(Short.parseShort((String) row.get(1)))
                                        .head(InvestmentType.LIC.name())
                                        .contribution(((String) row.get(5)).isEmpty() ? 0 : Integer.parseInt((String) row.get(5)))
                                        .contributionAsOnMonth(((String) row.get(6)).isEmpty() ? 0 : Integer.parseInt((String) row.get(6)))
                                        .valueAsOnMonth(((String) row.get(7)).isEmpty() ? 0 : Integer.parseInt((String) row.get(7)))
                                        .build(),
                                Investment.builder()
                                        .yearx(Short.parseShort((String) row.get(0)))
                                        .monthx(Short.parseShort((String) row.get(1)))
                                        .head(InvestmentType.NPS.name())
                                        .contribution(((String) row.get(8)).isEmpty() ? 0 : Integer.parseInt((String) row.get(8)))
                                        .contributionAsOnMonth(((String) row.get(9)).isEmpty() ? 0 : Integer.parseInt((String) row.get(9)))
                                        .valueAsOnMonth(((String) row.get(10)).isEmpty() ? 0 : Integer.parseInt((String) row.get(10)))
                                        .build(),
                                Investment.builder()
                                        .yearx(Short.parseShort((String) row.get(0)))
                                        .monthx(Short.parseShort((String) row.get(1)))
                                        .head(InvestmentType.SHARE.name())
                                        .contribution(((String) row.get(11)).isEmpty() ? 0 : Integer.parseInt((String) row.get(11)))
                                        .contributionAsOnMonth(((String) row.get(12)).isEmpty() ? 0 : Integer.parseInt((String) row.get(12)))
                                        .valueAsOnMonth(((String) row.get(13)).isEmpty() ? 0 : Integer.parseInt((String) row.get(13)))
                                        .build(),
                                Investment.builder()
                                        .yearx(Short.parseShort((String) row.get(0)))
                                        .monthx(Short.parseShort((String) row.get(1)))
                                        .head(InvestmentType.MF.name())
                                        .contribution(((String) row.get(14)).isEmpty() ? 0 : Integer.parseInt((String) row.get(14)))
                                        .contributionAsOnMonth(((String) row.get(15)).isEmpty() ? 0 : Integer.parseInt((String) row.get(15)))
                                        .valueAsOnMonth(((String) row.get(16)).isEmpty() ? 0 : Integer.parseInt((String) row.get(16)))
                                        .build()
                        )
                )
                .flatMap(Collection::stream)
                .toList();

        log.info("Number of transactions: {}", records.size());
        investmentRepository.deleteAll();
        investmentRepository.saveAll(records);
    }

    public void refreshOdionTransactionsData() throws IOException {
        initSheetService();
        ValueRange response = sheetsService.spreadsheets().values()
                .get(odionSheetId, odionTransactionsSheetRange)
                .execute();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<OdionTransaction> records = Optional.ofNullable(response.getValues()).orElse(Collections.emptyList()).stream()
                .filter(row -> row.size() == 5)
                .filter(row -> ((String) row.get(2)).length() != 0)
                .filter(row -> ((String) row.get(3)).length() != 0)
                .map(row -> OdionTransaction.builder()
                        .date(LocalDate.parse((String)row.get(0), formatter))
                        .particular((String)row.get(1))
                        .debitAccount(OdionTransaction.Account.valueOfOrDefault((String)row.get(2)))
                        .creditAccount(OdionTransaction.Account.valueOfOrDefault((String)row.get(3)))
                        .amount(Double.parseDouble((String) row.get(4)))
                        .build()
                )
                .toList();

        log.info("Number of transactions: {}", records.size());
        odionTransactionRepository.deleteAll();
        odionTransactionRepository.saveAll(records);
    }

    private Date parseToDate(String strDate) {
        try {
            return simpleDateFormat.parse(strDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}