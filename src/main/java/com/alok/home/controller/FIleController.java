package com.alok.home.controller;

import com.alok.home.batch.utils.Utility;
import com.alok.home.commons.constant.UploadType;
import com.alok.home.commons.utils.annotation.LogExecutionTime;
import com.alok.home.response.UploadFileResponse;
import com.alok.home.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/file")
public class FIleController {

    private FileStorageService fileStorageService;
    private JobExecutorOfExpenseService expenseJobExecutorService;
    private JobExecutorOfTaxService taxJobExecutorService;
    private JobExecutorOfInvestmentService investmentJobExecutorService;
    private JobExecutorOfBankService bankJobExecutorService;

    public FIleController(
            FileStorageService fileStorageService, JobExecutorOfExpenseService expenseJobExecutorService,
            JobExecutorOfTaxService taxJobExecutorService, JobExecutorOfInvestmentService investmentJobExecutorService,
            JobExecutorOfBankService bankJobExecutorService
    ) {
        this.fileStorageService = fileStorageService;
        this.expenseJobExecutorService = expenseJobExecutorService;
        this.taxJobExecutorService = taxJobExecutorService;
        this.investmentJobExecutorService = investmentJobExecutorService;
        this.bankJobExecutorService = bankJobExecutorService;
    }

    @LogExecutionTime
    @CrossOrigin
    @GetMapping("/processed")
    public ResponseEntity<Map<String, Object>> getAllProcessedFiles() {
        return ResponseEntity.ok()
                .body(fileStorageService.getAllProcessedFiles());
    }

    @LogExecutionTime
    @CrossOrigin
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UploadFileResponse> uploadStatement(
            @RequestParam MultipartFile file
    ) {

        log.info("Uploaded file: {}, type: {}, size: {}", file.getOriginalFilename(),
                file.getContentType(), file.getSize());

        UploadType uploadType = Utility.getUploadType(file.getOriginalFilename());

        String fineName = fileStorageService.storeFile(file, uploadType);

        try {
            if (uploadType == UploadType.ExpenseGoogleSheet)
                expenseJobExecutorService.executeAllJobs(true);

            if (uploadType == UploadType.TaxGoogleSheet)
                taxJobExecutorService.executeAllJobs(true);

            if (uploadType == UploadType.InvestmentGoogleSheet)
                investmentJobExecutorService.executeAllJobs(true);

            if (uploadType == UploadType.HDFCExportedStatement || uploadType == UploadType.KotakExportedStatement)
                bankJobExecutorService.executeBatchJob(uploadType, file.getOriginalFilename());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok()
                .body(
                        UploadFileResponse.builder()
                                .fileName(fineName)
                                .size(file.getSize())
                                .fileType(file.getContentType())
                                .message("File submitted for processing")
                                .uploadType(uploadType)
                                .build()
                );
    }
}
