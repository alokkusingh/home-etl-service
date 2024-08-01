package com.alok.home.controller;

import com.alok.home.model.EstateForm;
import com.alok.home.model.ExpenseForm;
import com.alok.home.response.GenericResponse;
import com.alok.home.service.FormService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/form")
public class FormController {

    private FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
    }

    @PostMapping("/expense")
    public ResponseEntity<GenericResponse> submitExpenseForm(@RequestBody ExpenseForm expenseForm) throws IOException {

        try {
            Assert.notNull(expenseForm.amount(), "Amount can't be null");
            Assert.notNull(expenseForm.head(), "Head can't be null");
        } catch (RuntimeException rte) {
            return ResponseEntity.badRequest()
                    .body(GenericResponse.builder()
                        .status(GenericResponse.Status.FAILED)
                        .message(rte.getMessage())
                        .build()
                    );
        }

        formService.submitExpenseForm(expenseForm);

        return ResponseEntity.ok()
                .body(GenericResponse.builder()
                        .status(GenericResponse.Status.SUCCESS)
                        .message("Expense entry created")
                        .build()
                );
    }

    @PostMapping("/estate")
    public ResponseEntity<GenericResponse> submitEstateForm(@RequestBody EstateForm estateForm) throws IOException {

        try {
            Assert.notNull(estateForm.amount(), "Amount can't be null");
            Assert.notNull(estateForm.particular(), "Particular can't be null");
            Assert.notNull(estateForm.debitFrom(), "DebitFrom can't be null");
            Assert.notNull(estateForm.creditTo(), "CreditTo can't be null");
        } catch (RuntimeException rte) {
            return ResponseEntity.badRequest()
                    .body(GenericResponse.builder()
                        .status(GenericResponse.Status.FAILED)
                        .message(rte.getMessage())
                        .build()
                    );
        }

        formService.submitEstateForm(estateForm);

        return ResponseEntity.ok()
                .body(GenericResponse.builder()
                    .status(GenericResponse.Status.SUCCESS)
                    .message("Transaction entry created")
                    .build()
                );
    }

}
