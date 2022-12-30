package com.alok.home.service;

import com.alok.home.batch.utils.Utility;
import com.alok.home.commons.constant.InvestmentType;
import com.alok.home.commons.model.*;
import com.alok.home.commons.repository.*;
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

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");

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
            OdionTransactionRepository odionTransactionRepository
    ) {
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
    }

    private void initSheetService() {
        if (sheetsService == null) {
            log.info("Google Sheet Service Initialized!");
            InputStream inputStream = null; // put your service account's key.json file in asset folder.
            try {
                inputStream = new FileInputStream(serviceAccountKeyFile);
            } catch (FileNotFoundException e) {
                log.error("Google Sheet initialization failed with error: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }


            GoogleCredentials googleCredentials = null;
            try {
                googleCredentials = GoogleCredentials.fromStream(inputStream)
                        .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));
            } catch (IOException e) {
                log.error("Google Sheet initialization failed with error: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);

            try {
                sheetsService = new Sheets.Builder(
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
        }
    }

    public void refreshTaxData() throws IOException {
        initSheetService();
        ValueRange response = sheetsService.spreadsheets().values()
                .get(expenseSheetId, taxSheetRange)
                .execute();

        List<Tax> records = Optional.ofNullable(response.getValues()).orElse(Collections.emptyList()).stream()
                .map(row -> Tax.builder()
                        .financialYear((String) row.get(0))
                        .paidAmount(row.size() < 2? 0: Integer.parseInt((String) row.get(1)))
                        .refundAmount(row.size() < 3? 0: Integer.parseInt((String) row.get(2)))
                        .build()
                )
                .toList();

        log.info("Number of transactions: {}", records.size());
        taxRepository.deleteAll();
        taxRepository.saveAll(records);
    }

    public void refreshTaxMonthlyData() throws IOException {
        initSheetService();
        ValueRange response = sheetsService.spreadsheets().values()
                .get(expenseSheetId, taxMonthSheetRange)
                .execute();

        List<TaxMonthly> records = Optional.ofNullable(response.getValues()).orElse(Collections.emptyList()).stream()
                .filter(row -> row.size() > 1)
                .map(row -> TaxMonthly.builder()
                        .yearMonth(YearMonth.parse((String)row.get(0)))
                        .paidAmount(Integer.parseInt((String) row.get(1)))
                        .build()
                )
                .toList();

        log.info("Number of transactions: {}", records.size());
        taxMonthlyRepository.deleteAll();
        taxMonthlyRepository.saveAll(records);
    }

    public void refreshExpenseData() throws IOException {
        initSheetService();
        ValueRange response = sheetsService.spreadsheets().values()
                .get(expenseSheetId, expenseSheetRange)
                .execute();

        List<Expense> records = Optional.ofNullable(response.getValues()).orElse(Collections.emptyList()).stream()
                .filter(row -> row.get(2) != null && ((String) row.get(2)).length() != 0)
                .map(row -> Expense.builder()
                        .date(parseToDate((String) row.get(0)))
                        .head((String) row.get(1))
                        .amount(Double.parseDouble((String) row.get(2)))
                        .comment(row.get(3) == null? "": (String) row.get(3))
                        .yearx(row.get(4) == null? 0:Integer.parseInt((String) row.get(4)))
                        .monthx(row.get(5) == null? 0:Integer.parseInt((String) row.get(5)))
                        .category(Utility.getExpenseCategory((String) row.get(1), row.get(3) == null? "": (String) row.get(3)))
                        .build()
                )
                .toList();

        log.info("Number of transactions: {}", records.size());
        expenseRepository.deleteAll();
        expenseRepository.saveAll(records);
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
                                        .contribution(((String) row.get(2)).length() == 0 ? 0 : Integer.parseInt((String) row.get(2)))
                                        .valueAsOnMonth(((String) row.get(4)).length() == 0 ? 0 : Integer.parseInt((String) row.get(4)))
                                        .build(),
                                Investment.builder()
                                        .yearx(Short.parseShort((String) row.get(0)))
                                        .monthx(Short.parseShort((String) row.get(1)))
                                        .head(InvestmentType.LIC.name())
                                        .contribution(((String) row.get(5)).length() == 0 ? 0 : Integer.parseInt((String) row.get(5)))
                                        .valueAsOnMonth(((String) row.get(7)).length() == 0 ? 0 : Integer.parseInt((String) row.get(7)))
                                        .build(),
                                Investment.builder()
                                        .yearx(Short.parseShort((String) row.get(0)))
                                        .monthx(Short.parseShort((String) row.get(1)))
                                        .head(InvestmentType.NPS.name())
                                        .contribution(((String) row.get(8)).length() == 0 ? 0 : Integer.parseInt((String) row.get(8)))
                                        .valueAsOnMonth(((String) row.get(10)).length() == 0 ? 0 : Integer.parseInt((String) row.get(10)))
                                        .build(),
                                Investment.builder()
                                        .yearx(Short.parseShort((String) row.get(0)))
                                        .monthx(Short.parseShort((String) row.get(1)))
                                        .head(InvestmentType.SHARE.name())
                                        .contribution(((String) row.get(11)).length() == 0 ? 0 : Integer.parseInt((String) row.get(11)))
                                        .valueAsOnMonth(((String) row.get(13)).length() == 0 ? 0 : Integer.parseInt((String) row.get(13)))
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