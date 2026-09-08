package com.alok.home.service.gsheet;

import com.alok.home.commons.constant.Account;
import com.alok.home.commons.entity.OdionTransaction;
import com.alok.home.commons.repository.InvestmentRepository;
import com.alok.home.commons.repository.OdionTransactionRepository;
import com.alok.home.service.GoogleSheetService;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class EstateSheetService extends GoogleSheetService {

    private final Sheets sheets;

    private final OdionTransactionRepository odionTransactionRepository;

    private final String sheetId;
    private final String odionTransactionsSheetRange;

    public EstateSheetService(
            Sheets sheets, InvestmentRepository investmentRepository,
            @Value("${sheet.id.odion}") String sheetId,
            @Value("${range.odion.transaction}") String odionTransactionsSheetRange,
            OdionTransactionRepository odionTransactionRepository
    ) {
        super();
        this.odionTransactionRepository = odionTransactionRepository;
        this.odionTransactionsSheetRange = odionTransactionsSheetRange;
        this.sheets = sheets;
        this.sheetId = sheetId;
    }

    @Override
    public void refreshSheet() throws IOException {
        ValueRange response = sheets.spreadsheets().values()
                .get(sheetId, odionTransactionsSheetRange)
                .execute();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<OdionTransaction> records = Optional.ofNullable(response.getValues()).orElse(Collections.emptyList()).stream()
                .filter(row -> row.size() == 5)
                .filter(row -> ((String) row.get(2)).length() != 0)
                .filter(row -> ((String) row.get(3)).length() != 0)
                .map(row -> OdionTransaction.builder()
                        .date(LocalDate.parse((String) row.get(0), formatter))
                        .particular((String) row.get(1))
                        .debitAccount(Account.valueOfOrDefault((String) row.get(2)))
                        .creditAccount(Account.valueOfOrDefault((String) row.get(3)))
                        .amount(Double.parseDouble((String) row.get(4)))
                        .build()
                )
                .toList();

        log.info("Number of transactions: {}", records.size());
        odionTransactionRepository.deleteAll();
        odionTransactionRepository.saveAll(records);
        log.info("Number of transactions: {} inserted", records.size());
    }
}
