package com.alok.home.batch.job;

import com.alok.home.batch.processor.FileArchiveTasklet;
import com.alok.home.batch.reader.CSVReader;
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
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
//@EnableBatchProcessing
public class ManualTransactionEntryBatchJob {
    @Value("file:${file.path.manual_account}")
    private Resource[] resources;

    @Value("${fields.name.manual_account:#{null}}")
    private String[] fieldNames;

    private static final String JOB_NAME = "";
    private static final String PROCESSOR_TASK_NAME = "";
    private static final String ARCHIVE_TASK_NAME = "";

    @Bean("ManualAccountJob")
    public Job manualAccountJob(JobRepository jobRepository,
                          PlatformTransactionManager transactionManager,
                          ItemReader<Transaction> manualItemsReader,
                          ItemProcessor<Transaction, Transaction> manualAccountProcessor,
                          ItemWriter<Transaction> bankAccountDbWriter
    ) {
        Step step1 = new StepBuilder("ManualAccount-ETL-Job-file-load", jobRepository)
                .<Transaction,Transaction>chunk(100, transactionManager)
                .reader(manualItemsReader)
                .processor(manualAccountProcessor)
                .writer(bankAccountDbWriter)
                .build();

        FileArchiveTasklet archiveTask = new FileArchiveTasklet();
        archiveTask.setResources(resources);
        Step step2 = new StepBuilder("ManualAccount-ETL-Job-file-archive", jobRepository)
                .tasklet(archiveTask, transactionManager)
                .build();

        return new JobBuilder("ManualAccount-ETL-Job", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1)
                .next(step2)
                .build();
    }



    @Bean
    public MultiResourceItemReader<Transaction> manualItemsReader(@Qualifier("manualItemReader") CSVReader manualItemReader) {

        MultiResourceItemReader<Transaction> reader = new MultiResourceItemReader<>();
        reader.setResources(resources);
        reader.setStrict(false);
        reader.setDelegate(manualItemReader);
        return reader;
    }

    @Bean
    public CSVReader<Transaction> manualItemReader(@Qualifier("CSVReader") CSVReader<Transaction> flatFileItemReader) {

        //FlatFileItemReader<Transaction> flatFileItemReader = new FlatFileItemReader<>();
        flatFileItemReader.setName("ManualTransaction-CSV-Reader");
        //flatFileItemReader.setLinesToSkip(1);
        flatFileItemReader.setComments(new String[] {"#"});
        flatFileItemReader.setLineMapper(manualLineMapper());
        flatFileItemReader.setStrict(false);
        //flatFileItemReader.setTransactionType("BANK");

        return flatFileItemReader;
    }

    @Bean
    public LineMapper<Transaction> manualLineMapper() {
        DefaultLineMapper<Transaction> defaultLineMapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames(fieldNames);

        BeanWrapperFieldSetMapper<Transaction> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Transaction.class);

        defaultLineMapper.setLineTokenizer(lineTokenizer);
        defaultLineMapper.setFieldSetMapper(fieldSetMapper);

        return defaultLineMapper;
    }
}

