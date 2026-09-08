package com.alok.home.service.gsheet;

import com.alok.home.commons.entity.FamilyProfile;
import com.alok.home.commons.entity.LifeEvent;
import com.alok.home.commons.repository.FamilyProfileRepository;
import com.alok.home.commons.repository.InvestmentRepository;
import com.alok.home.commons.repository.LifeEventRepository;
import com.alok.home.service.GoogleSheetService;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

@Slf4j
@Service
public class TimelineSheetService extends GoogleSheetService {

    private final Sheets sheets;
    private final String sheetId;
    private final String lifeEventSheetRange;

    private final LifeEventRepository lifeEventRepository;
    private final FamilyProfileRepository familyProfileRepository;

    private static final DateTimeFormatter SHEET_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("[d-MMM-yyyy][dd-MMM-yyyy][d MMM yyyy][dd MMM yyyy]")
            .toFormatter(Locale.ENGLISH);

    public TimelineSheetService(
            Sheets sheets, InvestmentRepository investmentRepository, LifeEventRepository lifeEventRepository,
            @Value("${sheet.id.events.life}") String sheetId,
            @Value("${range.events.life}") String lifeEventSheetRange, FamilyProfileRepository familyProfileRepository
    ) {
        super();
        this.lifeEventRepository = lifeEventRepository;
        this.lifeEventSheetRange = lifeEventSheetRange;
        this.sheets = sheets;
        this.sheetId = sheetId;
        this.familyProfileRepository = familyProfileRepository;
    }

    @Transactional
    @Override
    public void refreshSheet() throws IOException {
        ValueRange response = sheets.spreadsheets().values()
                .get(sheetId, lifeEventSheetRange)
                .execute();

        List<List<Object>> rawRows = Optional.ofNullable(response.getValues()).orElse(Collections.emptyList());

        // 1. Create a shallow copy to safely isolate the collection from underlying stream state changes
        List<List<Object>> rows = new ArrayList<>(rawRows);

        if (!rows.isEmpty() && rows.get(0).get(0).toString().equalsIgnoreCase("Date")) {
            rows.remove(0); // Safely pop the header
        }

        List<LifeEvent> recordsToSave = new ArrayList<>();

        // 2. Map everything purely in memory without saving to repositories mid-loop
        for (List<Object> row : rows) {
            if (row == null || row.isEmpty() || row.get(0) == null || row.get(0).toString().trim().isEmpty()) {
                continue;
            }

            try {
                LocalDate eventDate = LocalDate.parse(row.get(0).toString().trim(), SHEET_DATE_FORMATTER);

                String eventType = null;
                Set<String> participantNames = new HashSet<>();
                String[] entities = {"Alok", "Rachna", "Saanvi"};

                for (int i = 0; i < entities.length; i++) {
                    int columnIndex = i + 1;
                    if (row.size() > columnIndex && row.get(columnIndex) != null) {
                        String cellValue = row.get(columnIndex).toString().trim();
                        if (!cellValue.isEmpty()) {
                            eventType = cellValue;
                            participantNames.add(entities[i]);
                        }
                    }
                }

                if (eventType == null || eventType.isEmpty()) {
                    continue;
                }

                LocalDate endDate = null;
                if (row.size() > 4 && row.get(4) != null) {
                    String endDateStr = row.get(4).toString().trim();
                    if (!endDateStr.isEmpty()) {
                        endDate = LocalDate.parse(endDateStr, SHEET_DATE_FORMATTER);
                    }
                }

                String rawNotes = null;
                String externalId = null;
                for (int j = row.size() - 1; j >= 5; j--) {
                    if (row.get(j) != null) {
                        String cellContent = row.get(j).toString().trim();
                        if (!cellContent.isEmpty() && !cellContent.startsWith("Y:") && !cellContent.startsWith("Time:")) {
                            rawNotes = cellContent;
                            break;
                        } else if (cellContent.startsWith("Time:")) {
                            rawNotes = cellContent;
                            break;
                        }
                    }
                }

                if (rawNotes != null && rawNotes.contains("Id:")) {
                    externalId = rawNotes.replaceAll(".*Id:\\s*(\\S+).*", "$1");
                }

                LifeEvent lifeEvent = new LifeEvent();
                lifeEvent.setEventDate(eventDate);
                lifeEvent.setEndDate(endDate);
                lifeEvent.setEventType(eventType);
                lifeEvent.setNotes(rawNotes);
                lifeEvent.setExternalId(externalId);

                // Pre-fetch or assign names - we can fetch these during bulk loop safely
                for (String name : participantNames) {
                    FamilyProfile profile = familyProfileRepository.findByName(name)
                            .orElseGet(() -> {
                                FamilyProfile newProfile = new FamilyProfile();
                                newProfile.setName(name);
                                return familyProfileRepository.save(newProfile);
                            });
                    lifeEvent.addParticipant(profile);
                }

                recordsToSave.add(lifeEvent);

            } catch (Exception e) {
                log.error("Failed to parse row: {}, Error: ", row, e);
            }
        }

        // 3. DATABASE SYNC STAGE (Happens safely after all list iteration is complete)
        log.info("Total mapped rows to save: {}", recordsToSave.size());

        lifeEventRepository.deleteAll();
        lifeEventRepository.saveAll(recordsToSave);
        log.info("Successfully refreshed database tracking timeline!");
    }
}
