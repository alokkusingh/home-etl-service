package com.alok.home.service.gsheet;

import com.alok.home.commons.entity.MedicalLabResult;
import com.alok.home.commons.entity.MedicalMemberLab;
import com.alok.home.commons.entity.MedicalTestMetric;
import com.alok.home.commons.repository.MedicalMemberLabRepository;
import com.alok.home.commons.repository.MedicalTestMetricRepository;
import com.alok.home.service.GoogleSheetService;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
public class MedicalSheetService extends GoogleSheetService {

    private static final DateTimeFormatter SHEET_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("[d-MMM-yyyy][dd-MMM-yyyy][d MMM yyyy][dd MMM yyyy]")
            .toFormatter(Locale.ENGLISH);
    private final Sheets sheets;
    private final String sheetId;
    private final String medicalSheetRange;
    private final MedicalTestMetricRepository medicalTestMetricRepository;
    private final MedicalMemberLabRepository medicalMemberLabRepository;

    public MedicalSheetService(
            Sheets sheets,
            @Value("${sheet.id.medical}") String sheetId,
            @Value("${range.medical-sheet}") String medicalSheetRange,
            MedicalTestMetricRepository medicalTestMetricRepository,
            MedicalMemberLabRepository medicalMemberLabRepository
    ) {
        super();
        this.medicalSheetRange = medicalSheetRange;
        this.sheets = sheets;
        this.sheetId = sheetId;
        this.medicalTestMetricRepository = medicalTestMetricRepository;
        this.medicalMemberLabRepository = medicalMemberLabRepository;
    }


    @Transactional
    @Override
    public void refreshSheet() throws IOException {

       Stream.of(
                new AbstractMap.SimpleEntry<>("Alok", 1)
                ,new AbstractMap.SimpleEntry<>("Rachna", 2)
        ).forEach(entry -> {
            String sheetName = entry.getKey();
            Integer entityId = entry.getValue();
            try {
                String executionLockKey = ("medical-sync-lock-" + entityId).intern();

                // Fetch the entire matrix grid for this specific family member's sheet tab
                ValueRange response = sheets.spreadsheets().values()
                        .get(sheetId, sheetName + medicalSheetRange)
                        .execute();
                List<List<Object>> matrix = response.getValues();
                if (matrix == null || matrix.isEmpty()) return;

                // 1. Identify Date Columns (Row 0 / Header Row)
                List<Object> headerRow = matrix.get(0);
                Map<Integer, LocalDate> columnIndexToDateMap = new HashMap<>();

                for (int col = 4; col < headerRow.size(); col++) {
                    if (headerRow.get(col) == null) continue;
                    String cellStr = headerRow.get(col).toString().trim();
                    if (cellStr.isEmpty()) continue;

                    try {
                        LocalDate date = LocalDate.parse(cellStr, DateTimeFormatter.ofPattern("dd-M-yy"));
                        columnIndexToDateMap.put(col, date);
                    } catch (Exception e) {
                        log.debug("Skipped header column index {} with text: {}", col, cellStr);
                    }
                }

                // Map to pool dates cleanly into single unified records
                Map<LocalDate, MedicalMemberLab> trackingMap = new HashMap<>();
                // Set to track distinct metric IDs assigned per target date to prevent duplicate insertion crashes
                Map<LocalDate, Set<Integer>> duplicateMetricGuardMap = new HashMap<>();

                // 2. Loop vertically over the metric rows
                for (int rowIdx = 1; rowIdx < matrix.size(); rowIdx++) {
                    List<Object> row = matrix.get(rowIdx);
                    if (row == null || row.size() <= 1 || row.get(1) == null) continue;

                    String fullMetricName = row.get(1).toString().trim();
                    if (fullMetricName.isEmpty() || fullMetricName.equalsIgnoreCase("Test")) continue;

                    String rawMin = (row.size() > 2 && row.get(2) != null) ? row.get(2).toString().trim() : "";
                    String rawMax = (row.size() > 3 && row.get(3) != null) ? row.get(3).toString().trim() : "";

                    String extractedUnit = null;
                    if (fullMetricName.contains("(") && fullMetricName.contains(")")) {
                        extractedUnit = fullMetricName.substring(fullMetricName.lastIndexOf("(") + 1, fullMetricName.lastIndexOf(")"));
                    }

                    final String finalUnit = extractedUnit;

                    // Fetch or dynamically seed the master list entry for this metric
                    MedicalTestMetric metric = medicalTestMetricRepository.findByMetricName(fullMetricName)
                            .orElseGet(() -> {
                                MedicalTestMetric m = new MedicalTestMetric();
                                m.setMetricName(fullMetricName);
                                return m;
                            });

                    metric.setUnit(finalUnit);
                    try {
                        metric.setMinNormalValue(!rawMin.isEmpty() ? new BigDecimal(rawMin) : null);
                        metric.setMaxNormalValue(!rawMax.isEmpty() ? new BigDecimal(rawMax) : null);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid min/max numeric format on row {} for test: {}", rowIdx, fullMetricName);
                    }

                    metric = medicalTestMetricRepository.save(metric);
                    final Integer currentMetricId = metric.getMetricId();

                    // 3. Scan across columns starting from index 4 to pick up values
                    for (int colIdx = 4; colIdx < row.size(); colIdx++) {
                        if (!columnIndexToDateMap.containsKey(colIdx)) continue;
                        if (row.get(colIdx) == null || row.get(colIdx).toString().trim().isEmpty()) continue;

                        String rawVal = row.get(colIdx).toString().trim();
                        BigDecimal value;
                        try {
                            value = new BigDecimal(rawVal);
                        } catch (NumberFormatException e) {
                            continue; // Skip textual notes or anomalies safely
                        }

                        LocalDate targetDate = columnIndexToDateMap.get(colIdx);

                        // Check if this specific metric has already been recorded for this exact date
                        Set<Integer> recordedMetricsForDate = duplicateMetricGuardMap.computeIfAbsent(targetDate, k -> new HashSet<>());
                        if (recordedMetricsForDate.contains(currentMetricId)) {
                            log.warn("Duplicate metric '{}' detected for date {}. Skipping second value entry to prevent DB crash.", fullMetricName, targetDate);
                            continue; // Safely bypasses the crash condition
                        }

                        MedicalMemberLab labRecord = trackingMap.computeIfAbsent(targetDate, date -> {
                            MedicalMemberLab mml = new MedicalMemberLab();
                            mml.setEntityId(entityId);
                            mml.setTestDate(date);
                            return mml;
                        });

                        MedicalLabResult result = new MedicalLabResult();
                        result.setMetric(metric);
                        result.setRecordedValue(value);
                        result.setIsOutOfRange(checkIfValueIsOutBounds(value, metric));

                        labRecord.addResult(result);

                        // Flag this metric ID as processed for this date
                        recordedMetricsForDate.add(currentMetricId);
                    }
                }

                // 4. DATABASE SYNC BLOCK
                medicalMemberLabRepository.deleteByEntityId(Long.valueOf(entityId));
                medicalMemberLabRepository.flush();

                medicalMemberLabRepository.saveAll(trackingMap.values());
                log.info("Successfully refreshed timeline mapping logs for entity ID: {}", entityId);
            } catch (Exception e) {
                log.error("Error refreshing medical report for {}: {}", entry.getKey(), e.getMessage());
            }
        });

    }


    /**
     * Compares the recorded medical value against the metric's normal min/max boundaries.
     *
     * @param recordedValue The numeric result from the lab sheet.
     * @param metric        The metadata containing the normal reference ranges.
     * @return True if the value falls outside the normal range, false otherwise.
     */
    private Boolean checkIfValueIsOutBounds(BigDecimal recordedValue, MedicalTestMetric metric) {
        if (recordedValue == null || metric == null) {
            return false;
        }

        BigDecimal min = metric.getMinNormalValue();
        BigDecimal max = metric.getMaxNormalValue();

        // 1. Check if it drops below the minimum normal range threshold
        if (min != null && recordedValue.compareTo(min) < 0) {
            return true;
        }

        // 2. Check if it spikes above the maximum normal range threshold
        if (max != null && recordedValue.compareTo(max) > 0) {
            return true;
        }

        // Value sits comfortably within bounds
        return false;
    }

    /**
     * Extracts and maps boundary limits from typical sheet ranges (e.g., "70-100", "<6.5", "12.5 - 15.5")
     */
    /**
     * Extracts and maps boundary limits from typical sheet ranges (e.g., "70-100", "<6.5", "12.5 - 15.5")
     */
    private void parseAndPopulateRanges(MedicalTestMetric metric, String rawRange) {
        if (rawRange == null || rawRange.isEmpty() || rawRange.equalsIgnoreCase("null")) {
            metric.setMinNormalValue(null);
            metric.setMaxNormalValue(null);
            return;
        }
        try {
            // Strip out whitespace and normalize all dash variants to standard hyphens
            String cleanRange = rawRange.replaceAll("\\s+", "").replace("–", "-");

            if (cleanRange.contains("-")) {
                String[] parts = cleanRange.split("-");
                if (parts.length == 2) {
                    // Explicitly pull array strings and trim before converting to BigDecimal
                    String minStr = parts[0].trim();
                    String maxStr = parts[1].trim();

                    metric.setMinNormalValue(new BigDecimal(minStr));
                    metric.setMaxNormalValue(new BigDecimal(maxStr));
                }
            } else if (cleanRange.startsWith("<=")) {
                metric.setMinNormalValue(null);
                metric.setMaxNormalValue(new BigDecimal(cleanRange.replace("<=", "")));
            } else if (cleanRange.startsWith("<")) {
                metric.setMinNormalValue(null);
                metric.setMaxNormalValue(new BigDecimal(cleanRange.replace("<", "")));
            } else if (cleanRange.startsWith(">=")) {
                metric.setMinNormalValue(new BigDecimal(cleanRange.replace(">=", "")));
                metric.setMaxNormalValue(null);
            } else if (cleanRange.startsWith(">")) {
                metric.setMinNormalValue(new BigDecimal(cleanRange.replace(">", "")));
                metric.setMaxNormalValue(null);
            }
        } catch (Exception e) {
            log.warn("Could not parse reference range definition '{}' for metric: {}", rawRange, metric.getMetricName());
        }
    }
}
