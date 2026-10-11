package com.alok.home.batch.job.kotak;

import com.alok.home.batch.processor.FileArchiveTasklet;
import com.alok.home.batch.reader.CSVReader;
import com.alok.home.batch.utils.BankUtils;
import com.alok.home.commons.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
//@EnableBatchProcessing
public class KotakImportedAccountStatementBatchJobV3 {
    @Value("file:${file.path.kotak_account.imported.v3}")
    private Resource[] resources;

    @Value("${fields.name.kotak_account.imported.v3:#{null}}")
    private String[] fieldNames;

    private static final String JOB_NAME = "KotakAccount-Imported-ETL-Job5";
    private static final String PROCESSOR_TASK_NAME = "KotakAccount-Imported-ETL-Job5-file-load";
    private static final String ARCHIVE_TASK_NAME = "KotakAccount-Imported-ETL-Job5-file-archive";

    @Bean("KotakImportedAccountJobV3")
    public Job kotakImportedAccountJobV3(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         ItemReader<Transaction> kotakImportedItemsReaderV3,
                                         ItemProcessor<Transaction, Transaction> defaultAccountProcessor,
                                         ItemWriter<Transaction> bankAccountDbWriter
    ) {
        Step step1 = new StepBuilder(PROCESSOR_TASK_NAME, jobRepository)
                .<Transaction,Transaction>chunk(100, transactionManager)
                .reader(kotakImportedItemsReaderV3)
                .processor(defaultAccountProcessor)
                .writer(bankAccountDbWriter)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skipLimit(100)
                .listener(new SkipListener<Transaction, Transaction>() {
                    @Override
                    public void onSkipInRead(Throwable t) {
                        if (t instanceof FlatFileParseException) {
                            FlatFileParseException ffpe = (FlatFileParseException) t;
                            log.warn("Skipped bad line number: {} with input: {}", ffpe.getLineNumber(), ffpe.getInput());
                        }
                    }

                    @Override
                    public void onSkipInWrite(Transaction item, Throwable t) {
                        log.warn("Skipped item: {} with error: {}", item, t.getMessage());
                    }

                    @Override
                    public void onSkipInProcess(Transaction item, Throwable t) {
                        log.warn("Skipped item: {} with error: {}", item, t.getMessage());
                    }
                })
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
    public MultiResourceItemReader<Transaction> kotakImportedItemsReaderV3(@Qualifier("kotakImportedItemReaderV3") CSVReader kotakImportedItemReaderV3) {

        MultiResourceItemReader<Transaction> reader = new MultiResourceItemReader<>();
        reader.setResources(resources);
        reader.setStrict(false);
        reader.setDelegate(kotakImportedItemReaderV3);
        return reader;
    }

    @Bean
    public CSVReader<Transaction> kotakImportedItemReaderV3(@Qualifier("CSVReader") CSVReader<Transaction> flatFileItemReader) {

        // return KotakUtils.kotakImportedItemReader(fieldNames);
        flatFileItemReader.setName("KotakImportedAccount-CSV-Reader");
        flatFileItemReader.setLineMapper(BankUtils.importedAccountLineMapper(fieldNames, BankUtils.LineMapperType.KOTAK_V3));
        flatFileItemReader.setStrict(false);
        flatFileItemReader.setComments(new String[] {",", "\"\"", "#",
                "ALOK", "\"ALOK", "Bangalore", "\"BANGALORE", "Karnataka", "\"KARNATAKA", "INDIA", "\"INDIA", "Opening", "\"OPENING", "Closing", "\"Closing", "You", "\"You",
                "202", "\"202", "Doddakannalli", "\"DODDAKANNALLI", "SArjapur", "\"SARJAPUR", "Bengaluru", "\"BENGALURU", "Karnataka", "\"Karnataka", "India", "\"India", "560035", "\"560035"
        });
        flatFileItemReader.setLinesToSkip(1);
        flatFileItemReader.setTransactionType("BANK");

        return flatFileItemReader;
    }
}