package com.alok.home.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfig {

    @Bean
    public RestTemplate gitHubClient(@Value("${git.bearer-token}") String token) {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, clientHttpRequestExecution) -> {
            HttpHeaders headers = request.getHeaders();
            headers.add("Content-Type", "application/json");
            if (!headers.containsKey("Authorization")) {
                request.getHeaders().add("Authorization", "Bearer " + token);
            }
            return clientHttpRequestExecution.execute(request, body);
        });

        return restTemplate;
    }
}
