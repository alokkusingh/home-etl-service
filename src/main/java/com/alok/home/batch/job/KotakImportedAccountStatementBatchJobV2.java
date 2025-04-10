package com.alok.home.batch.job;

import com.alok.home.batch.processor.FileArchiveTasklet;
import com.alok.home.batch.reader.CSVReader;
import com.alok.home.batch.utils.BankUtils;
import com.alok.home.commons.entity.Transaction;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
//@EnableBatchProcessing
public class KotakImportedAccountStatementBatchJobV2 {
    @Value("file:${file.path.kotak_account.imported.v2}")
    private Resource[] resources;

    @Value("${fields.name.kotak_account.imported.v2:#{null}}")
    private String[] fieldNames;

    private static final String JOB_NAME = "KotakAccount-Imported-ETL-Job4";
    private static final String PROCESSOR_TASK_NAME = "KotakAccount-Imported-ETL-Job4-file-load";
    private static final String ARCHIVE_TASK_NAME = "KotakAccount-Imported-ETL-Job4-file-archive";

    @Bean("KotakImportedAccountJobV2")
    public Job kotakImportedAccountJobV2(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         ItemReader<Transaction> kotakImportedItemsReaderV2,
                                         ItemProcessor<Transaction, Transaction> defaultAccountProcessor,
                                         ItemWriter<Transaction> bankAccountDbWriter
    ) {
        Step step1 = new StepBuilder(PROCESSOR_TASK_NAME, jobRepository)
                .<Transaction,Transaction>chunk(100, transactionManager)
                .reader(kotakImportedItemsReaderV2)
                .processor(defaultAccountProcessor)
                .writer(bankAccountDbWriter)
                .build();

        FileArchiveTasklet archiveTask = new FileArchiveTasklet();
        archiveTask.setResources(resources);
        Step step2 = new StepBuilder(ARCHIVE_TASK_NAME, jobRepository)
                .tasklet(archiveTask, transactionManager)
                .build();

        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1)
                .next(step2)
                .build();
    }

    @Bean
    public MultiResourceItemReader<Transaction> kotakImportedItemsReaderV2(@Qualifier("kotakImportedItemReaderV2") CSVReader kotakImportedItemReaderV2) {

        MultiResourceItemReader<Transaction> reader = new MultiResourceItemReader<>();
        reader.setResources(resources);
        reader.setStrict(false);
        reader.setDelegate(kotakImportedItemReaderV2);
        return reader;
    }

    @Bean
    public CSVReader<Transaction> kotakImportedItemReaderV2(@Qualifier("CSVReader") CSVReader<Transaction> flatFileItemReader) {

        // return KotakUtils.kotakImportedItemReader(fieldNames);
        flatFileItemReader.setName("KotakImportedAccount-CSV-Reader");
        flatFileItemReader.setLineMapper(BankUtils.importedAccountLineMapper(fieldNames, BankUtils.LineMapperType.KOTAK));
        flatFileItemReader.setStrict(false);
        flatFileItemReader.setComments(new String[] {",", "\"", "#",
                "ALOK", "Bangalore", "KARNATAKA", "INDIA", "Opening", "Closing", "You",
                "202", "Doddakannalli", "SArjapur", "Bengaluru", "Karnataka", "India", "560035"
        });
        flatFileItemReader.setLinesToSkip(1);
        flatFileItemReader.setTransactionType("BANK");

        return flatFileItemReader;
    }
}