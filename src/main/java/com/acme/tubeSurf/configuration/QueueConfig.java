package com.acme.tubeSurf.configuration;

import com.acme.tubeSurf.model.operation.CommentChunkRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class QueueConfig {
    @Bean
    public BlockingQueue<CommentChunkRequest> urlQueue(){
        return new LinkedBlockingQueue<CommentChunkRequest>();
    }

    @Bean
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("CommentsHandleExecutor-");
        executor.initialize();
        return executor;
    }
}
