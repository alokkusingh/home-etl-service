package com.alok.home.service;

import com.alok.home.commons.constant.BatchOf;
import com.alok.home.commons.repository.ProcessedFileRepository;
import com.alok.home.commons.repository.TaxRepository;
import com.alok.home.commons.utils.annotation.LogExecutionTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobExecutorOfTaxService {

    private JobLauncher jobLauncher;
    private Job taxJob;
    private TaxRepository taxRepository;
    private ProcessedFileRepository processedFileRepository;

    public JobExecutorOfTaxService(
            JobLauncher jobLauncher, @Qualifier("TaxJob") Job taxJob,
            TaxRepository taxRepository, ProcessedFileRepository processedFileRepository
    ) {
        this.jobLauncher = jobLauncher;
        this.taxJob = taxJob;
        this.taxRepository = taxRepository;
        this.processedFileRepository = processedFileRepository;
    }

    public void executeAllJobs() throws Exception {
        executeAllJobs(false);
    }

    @LogExecutionTime
    public void executeAllJobs(boolean force) throws Exception {

        if (force) {
            log.info("Delete all the taxes first");
            taxRepository.deleteAll();
            processedFileRepository.deleteAllByType("TAX");
        }

        log.info("Starting job execution");

        jobLauncher.run(taxJob, new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .addString("batchOf", BatchOf.TAX.name())
                .toJobParameters());

        log.debug("Completed job execution");
    }
}
