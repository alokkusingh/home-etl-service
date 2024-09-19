package com.alok.home.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;

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

    @Bean
    public ExecutorService virtualThreadExecutorService() {
        final ThreadFactory factory = Thread.ofVirtual().name("v-thread-", 0).factory();
        return Executors.newThreadPerTaskExecutor(factory);
        //return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public ExecutorService cachedThreadPoolExecutorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public ExecutorService fixedThreadPoolExecutorService() {
        return Executors.newFixedThreadPool(5);
    }

    @Bean
    public ExecutorService forkJoinPoolExecutorService() {
      return ForkJoinPool.commonPool();
    }

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        // When a new task is submitted in method execute(Runnable),
        //and fewer than corePoolSize threads are running, a new thread is
        //created to handle the request, even if other worker threads are
        //idle.  If there are more than corePoolSize but less than
        //maximumPoolSize threads running, a new thread will be created only
        //if the queue is full.
        taskExecutor.setCorePoolSize(5);    // minimum number of workers to keep alive
        taskExecutor.setMaxPoolSize(7);     // maximum number of threads that can ever be created
        taskExecutor.setQueueCapacity(10);   // In the event of a full queue and the max pool size has been reached. The next task submitted for execution will be rejected according to the configured RejectedExecutionHandler. By default this is the AbortPolicy which throws an error. Other pre-existing handers include CallerRunsPolicy, DiscardPolicy and DiscardOldestPolicy.
        // This would mean the first 5 tasks would be started on threads, after which all tasks will be queued untill the capacity of 10 is reached. Only then will new threads be started for newly submitted tasks, which hopefully leads to a decrease of tasks in the queue.

        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setThreadNamePrefix("tpool-");
        taskExecutor.initialize();

        return taskExecutor;
    }
}