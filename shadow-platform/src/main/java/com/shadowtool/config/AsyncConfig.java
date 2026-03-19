package com.shadowtool.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.thread-pool.core-size:10}")
    private int coreSize;

    @Value("${async.thread-pool.max-size:50}")
    private int maxSize;

    @Value("${async.thread-pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${async.thread-pool.thread-name-prefix:shadow-async-}")
    private String threadNamePrefix;

    @Bean(name = "shadowTaskExecutor")
    public Executor shadowTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
