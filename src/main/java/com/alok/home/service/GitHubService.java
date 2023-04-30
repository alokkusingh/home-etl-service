package com.alok.home.service;

import com.alok.home.model.GitPostContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Service
public class GitHubService {


    @Autowired
    private RestTemplate gitHubClient;
    private String gitURL = "https://api.github.com/repos/alokkusingh/BankStatements/contents/";

    public void uploadFile(String fileName, byte[] content, String message) {
        log.info("Github - uploading file {}", fileName);
        gitHubClient.put(
                gitURL + (fileName.startsWith("/") ? fileName.substring(1) : fileName),
                new GitPostContent(
                        message,
                        Base64.getEncoder().encodeToString(content)
                )
        );
        log.info("Github - upload completed");
    }

}
