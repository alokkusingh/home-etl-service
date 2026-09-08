package com.alok.home.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Configuration
public class SheetConfig {

    private final String serviceAccountKeyFile;
    private final ExecutorService virtualThreadExecutorService;

    public SheetConfig(
            @Value("${file.path.service_account.key}") String serviceAccountKeyFile,
            ExecutorService virtualThreadExecutorService
    ) {
        this.serviceAccountKeyFile = serviceAccountKeyFile;
        this.virtualThreadExecutorService = virtualThreadExecutorService;
    }

    @Bean
    public Sheets sheets() {
        Sheet sheet = null;
        log.info("Google Sheet Service Initializing!");

        return CompletableFuture.supplyAsync(() -> {
                    try {
                        log.info(Thread.currentThread().toString());
                        log.info("Reading credentials file");
                        return new FileInputStream(serviceAccountKeyFile);
                    } catch (FileNotFoundException e) {
                        log.error("Google Sheet initialization failed with error: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }, virtualThreadExecutorService)
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                })
                .thenApply(inputStream -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Creating Http Credentials Adaptor");
                    try {
                        return new HttpCredentialsAdapter(GoogleCredentials.fromStream(inputStream)
                                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY)));
                    } catch (IOException e) {
                        log.error("Google Sheet initialization failed with error: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                })
                .thenApply(requestInitializer -> {
                    log.info(Thread.currentThread().toString());
                    log.info("Creating Sheet Builder");
                    try {
                        return new Sheets.Builder(
                                GoogleNetHttpTransport.newTrustedTransport(),
                                //GsonFactory.getDefaultInstance(),
                                JacksonFactory.getDefaultInstance(),
                                requestInitializer
                        )
                                .setApplicationName("Home Stack")
                                .build();
                    } catch (GeneralSecurityException | IOException | RuntimeException e) {
                        log.error("Google Sheet initialization failed with error: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                })
                .join();
    }
}

