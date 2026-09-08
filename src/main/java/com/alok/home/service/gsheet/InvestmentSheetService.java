package com.alok.home.service.gsheet;

import com.alok.home.commons.constant.InvestmentType;
import com.alok.home.commons.entity.Investment;
import com.alok.home.commons.repository.ExpenseRepository;
import com.alok.home.commons.repository.InvestmentRepository;
import com.alok.home.grpc.ExpenseCategorizerClient;
import com.alok.home.service.GoogleSheetService;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class InvestmentSheetService extends GoogleSheetService {

    private final Sheets sheets;

    private final InvestmentRepository investmentRepository;

    private final String sheetId;
    private final String investmentSheetRange;

    public InvestmentSheetService(
            Sheets sheets, InvestmentRepository investmentRepository,
            @Value("${sheet.id.expense}") String sheetId,
            @Value("${range.investment-sheet}") String investmentSheetRange
    ) {
        super();
        this.investmentRepository = investmentRepository;
        this.investmentSheetRange = investmentSheetRange;
        this.sheets = sheets;
        this.sheetId = sheetId;
    }

    @Override
    public void refreshSheet() throws IOException {
        ValueRange response = sheets.spreadsheets().values()
                .get(sheetId, investmentSheetRange)
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
                                        .build(),
                                Investment.builder()
                                        .yearx(Short.parseShort((String) row.get(0)))
                                        .monthx(Short.parseShort((String) row.get(1)))
                                        .head(InvestmentType.ESPP.name())
                                        .contribution(((String) row.get(17)).isEmpty() ? 0 : Integer.parseInt((String) row.get(17)))
                                        .contributionAsOnMonth(((String) row.get(18)).isEmpty() ? 0 : Integer.parseInt((String) row.get(18)))
                                        .valueAsOnMonth(((String) row.get(19)).isEmpty() ? 0 : Integer.parseInt((String) row.get(19)))
                                        .build()
                        )
                )
                .flatMap(Collection::stream)
                .toList();

        log.info("Number of transactions: {}", records.size());
        investmentRepository.deleteAll();
        investmentRepository.saveAll(records);
        log.info("Number of Investment transactions: {} inserted", records.size());
    }
}
