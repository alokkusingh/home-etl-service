package com.alok.home.batch.job;

import com.alok.home.batch.processor.FileArchiveTasklet;
import com.alok.home.batch.reader.PDFReader;
import com.alok.home.commons.entity.RawTransaction;
import com.alok.home.commons.entity.Transaction;
import com.alok.home.commons.repository.ProcessedFileRepository;
import com.alok.home.batch.utils.DefaultLineExtractor;
import com.alok.home.batch.utils.LineExtractor;
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
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
//@EnableBatchProcessing
public class CitiAccountStatementBatchJob2 {
    @Value("file:${file.path.citi_account.password2}")
    private Resource[] resources;

    @Value("${file.password.citi.password2}")
    private String filePassword;

    private ProcessedFileRepository processedFileRepository;

    private static final String JOB_NAME = "CitiAccount-ETL-Job2";
    private static final String PROCESSOR_TASK_NAME = "CitiAccount-ETL-Job2-file-load";
    private static final String ARCHIVE_TASK_NAME = "CitiAccount-ETL-Job2-file-archive";

    @Bean("CitiBankJob2")
    public Job citiBankJob1(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            ItemReader<RawTransaction> citiItemsReader2,
                            ItemProcessor<RawTransaction, Transaction> citiBankAccountProcessor,
                            ItemWriter<Transaction> bankAccountDbWriter,
                            ProcessedFileRepository processedFileRepository
    ) {
        this.processedFileRepository = processedFileRepository;

        Step step1 = new StepBuilder(PROCESSOR_TASK_NAME, jobRepository)
                .<RawTransaction,Transaction>chunk(100, transactionManager)
                .reader(citiItemsReader2)
                .processor(citiBankAccountProcessor)
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
    public MultiResourceItemReader<RawTransaction> citiItemsReader2(PDFReader citiItemReader2) {

        MultiResourceItemReader<RawTransaction> reader = new MultiResourceItemReader<>();
        reader.setResources(resources);
        reader.setStrict(false);
        reader.setDelegate(citiItemReader2);
        return reader;
    }

    @Bean
    @DependsOn({"processedFileRepository"})
    public PDFReader citiItemReader2(@Qualifier("PDFReader") PDFReader flatFileItemReader) {

        //return CitiUtils.getCitiItemReader(filePassword, processedFileRepository);
        //PDFReader flatFileItemReader = new PDFReader(processedFileRepository);
        flatFileItemReader.setName("CitiBank-PDF-Reader2");
        flatFileItemReader.setFilePassword(filePassword);

        LineExtractor defaultLineExtractor = new DefaultLineExtractor();
        defaultLineExtractor.setStartReadingText("Date Transaction.*");
        defaultLineExtractor.setEndReadingText("Banking Reward Points.*");
        defaultLineExtractor.setLinesToSkip(
                new String[] {
                        "^Your  Citibank  Account.*",
                        "^Statement  Period.*",
                        "^Page .*"
                }
        );

        flatFileItemReader.setLineExtractor(defaultLineExtractor);

        return flatFileItemReader;
    }
}
