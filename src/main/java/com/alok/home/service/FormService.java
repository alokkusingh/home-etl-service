package com.alok.home.service;

import com.alok.home.commons.model.Expense;
import com.alok.home.commons.model.OdionTransaction;
import com.alok.home.commons.repository.ExpenseRepository;
import com.alok.home.commons.repository.OdionTransactionRepository;
import com.alok.home.grpc.ExpenseCategorizerClient;
import com.alok.home.model.EstateForm;
import com.alok.home.model.ExpenseForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;

@Service
public class FormService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategorizerClient expenseCategorizerClient;
    private final OdionTransactionRepository odionTransactionRepository;

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

    public void submitExpenseForm(ExpenseForm expenseForm) throws IOException {

        restTemplate.getForEntity(
                String.format(
                        expenseFormUrl,
                        URLEncoder.encode(expenseForm.head(), StandardCharsets.UTF_8),
                        expenseForm.amount(),
                        URLEncoder.encode(expenseForm.comment() == null?"":expenseForm.comment(), StandardCharsets.UTF_8)
                ),
                String.class
        );


        expenseRepository.save(
                Expense.builder()
                    .date(new Date())
                    .head(expenseForm.head())
                    .amount(expenseForm.amount())
                    .comment(expenseForm.comment() == null? "": expenseForm.comment())
                    .yearx(YearMonth.now().getYear())
                    .monthx(YearMonth.now().getMonthValue())
                    .category(expenseCategorizerClient.getExpenseCategory(expenseForm.head()))
                    .build()
                );
    }

    public void submitEstateForm(EstateForm estateForm) throws IOException {

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
                        .debitAccount(OdionTransaction.Account.valueOfOrDefault(estateForm.debitFrom()))
                        .creditAccount(OdionTransaction.Account.valueOfOrDefault(estateForm.creditTo()))
                        .amount(estateForm.amount())
                        .build()
        );
    }
}
