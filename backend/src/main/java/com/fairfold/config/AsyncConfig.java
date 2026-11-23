package com.fairfold.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Resume text extraction and AI calls run off the request thread. A small
     * pool is enough: each candidate has one current resume and uploads are
     * rare compared with reads.
     */
    @Bean("resumeExecutor")
    public Executor resumeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("resume-");
        executor.initialize();
        return executor;
    }

    /** One run per thread; up to four jobs can screen at the same time. */
    @Bean("screeningExecutor")
    public Executor screeningExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("screening-");
        executor.initialize();
        return executor;
    }
}
