package com.alok.home.service;

import com.alok.home.commons.constant.Account;
import com.alok.home.commons.entity.Expense;
import com.alok.home.commons.entity.OdionTransaction;
import com.alok.home.commons.repository.ExpenseRepository;
import com.alok.home.commons.repository.OdionTransactionRepository;
import com.alok.home.grpc.ExpenseCategorizerClient;
import com.alok.home.model.EstateForm;
import com.alok.home.model.ExpenseForm;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FormService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategorizerClient expenseCategorizerClient;
    private final OdionTransactionRepository odionTransactionRepository;
    private final ConcurrentHashMap<String, Boolean> idempotencyCache = new ConcurrentHashMap<>();

    private final RestTemplate restTemplate = new RestTemplate();
    private final String estateFormUrl;
    private final String expenseFormUrl;

    public FormService(
            ExpenseRepository expenseRepository,
            ExpenseCategorizerClient expenseCategorizerClient,
            OdionTransactionRepository odionTransactionRepository,
            @Value("${form.expense.url}") String expenseFormUrl,
            @Value("${form.estate.url}") String estateFormUrl
    ) {
        this.expenseRepository = expenseRepository;
        this.expenseCategorizerClient = expenseCategorizerClient;
        this.odionTransactionRepository = odionTransactionRepository;
        this.estateFormUrl = estateFormUrl;
        this.expenseFormUrl = expenseFormUrl;
    }

    public void submitExpenseForm(ExpenseForm expenseForm, String idempotencyKey) throws IOException {

        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            return;
        }

        expenseRepository.save(
                Expense.builder()
                    .date(new Date())
                    .head(expenseForm.head())
                    .amount(expenseForm.amount())
                    .comment(expenseForm.comment() == null? "": StringUtils.abbreviate(expenseForm.comment(), 255))
                    .yearx(YearMonth.now().getYear())
                    .monthx(YearMonth.now().getMonthValue())
                    .category(expenseCategorizerClient.getExpenseCategory(expenseForm.head()))
                    .build()
                );

        restTemplate.getForEntity(
                String.format(
                        expenseFormUrl,
                        URLEncoder.encode(expenseForm.head(), StandardCharsets.UTF_8),
                        expenseForm.amount(),
                        URLEncoder.encode(expenseForm.comment() == null?"":StringUtils.abbreviate(expenseForm.comment(), 255), StandardCharsets.UTF_8)
                ),
                String.class
        );

        if (idempotencyKey != null) {
            idempotencyCache.put(idempotencyKey, true);
        }
    }

    public void submitEstateForm(EstateForm estateForm, String idempotencyKey) throws IOException {

        if (idempotencyKey != null && idempotencyCache.containsKey(idempotencyKey)) {
            return;
        }

        restTemplate.getForEntity(
                String.format(estateFormUrl,
                        URLEncoder.encode(estateForm.particular(), StandardCharsets.UTF_8),
                        URLEncoder.encode(estateForm.debitFrom(), StandardCharsets.UTF_8),
                        URLEncoder.encode(estateForm.creditTo(), StandardCharsets.UTF_8),
                        estateForm.amount()
                ),
                String.class
        );

        odionTransactionRepository.save(
                OdionTransaction.builder()
                        .date(LocalDate.now())
                        .particular(estateForm.particular())
                        .debitAccount(Account.valueOfOrDefault(estateForm.debitFrom()))
                        .creditAccount(Account.valueOfOrDefault(estateForm.creditTo()))
                        .amount(estateForm.amount())
                        .build()
        );

        if (idempotencyKey != null) {
            idempotencyCache.put(idempotencyKey, true);
        }
    }
}
