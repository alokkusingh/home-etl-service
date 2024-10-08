package com.alok.home.service;

import com.alok.home.commons.constant.BatchOf;
import com.alok.home.commons.repository.ExpenseRepository;
import com.alok.home.commons.repository.ProcessedFileRepository;
import com.alok.home.commons.utils.annotation.LogExecutionTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobExecutorOfExpenseService {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("ExpenseJob")
    private Job expenseJob;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ProcessedFileRepository processedFileRepository;

    public void executeAllJobs() throws Exception {
        executeAllJobs(false);
    }

    @LogExecutionTime
    public void executeAllJobs(boolean force) throws Exception {

        if (force) {
            log.info("Delete all the expenses first");
            expenseRepository.deleteAll();
            processedFileRepository.deleteAllByType("EXPENSE");
        }

        log.info("Starting job execution");

        jobLauncher.run(expenseJob, new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .addString("batchOf", BatchOf.EXPENSE.name())
                .toJobParameters());

        log.debug("Completed job execution");
    }
}
