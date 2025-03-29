package com.alok.home.batch.reader;

import com.alok.home.commons.entity.ProcessedFile;
import com.alok.home.commons.entity.Transaction;
import com.alok.home.commons.repository.ProcessedFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CSVReader<T> extends FlatFileItemReader<T> {
    private Resource resource;
    private String transactionType;
    private final ProcessedFileRepository processedFileRepository;

    public CSVReader(ProcessedFileRepository processedFileRepository, LineMapper<T> lineMapper) {
        this.processedFileRepository = processedFileRepository;
        setLineMapper(lineMapper);
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        log.info("Started Processing File: {}", resource);
        List<ProcessedFile> processedFile = processedFileRepository.findAllByName(resource.getFilename());
        if (!processedFile.isEmpty()) {
            log.warn("CSV File already processed - skipping!");
            return;
        }
        super.open(executionContext);
    }

    @Override
    protected T doRead() throws Exception {
        List<ProcessedFile> processedFile = processedFileRepository.findAllByName(resource.getFilename());
        if (!processedFile.isEmpty()) {
            return null;
        }

        T t = super.doRead();

        if (t instanceof Transaction transaction) {
            log.debug("Read: {}", t);
            transaction.setFile(resource.getFilename());
        }

        return t;
    }

    @Override
    public void setResource(Resource resource) {
        super.setResource(resource);
        this.resource = resource;
    }

    @Override
    public void close() throws ItemStreamException {
        super.close();
        if (resource != null) {
            log.debug("Finished Processing File: {}", resource);
            List<ProcessedFile> processedFile = processedFileRepository.findAllByName(resource.getFilename());
            if (processedFile.isEmpty()) {
                processedFileRepository.save(
                        ProcessedFile.builder()
                                .name(resource.getFilename())
                                .date(new Date())
                                .type(transactionType)
                                .build()
                );
            }
        }
    }

}
