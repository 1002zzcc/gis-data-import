package com.zjxy.gisdataimport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

/**
 * 批量处理配置类 - 优化大数据量处理性能
 */
@Configuration
public class BatchProcessingConfig {
    
    /**
     * 批量处理常量配置
     */
    public static class BatchConstants {
        // 大批次大小 - 每个线程处理的数据量
        public static final int LARGE_BATCH_SIZE = 10000;
        
        // JDBC批次大小 - 每次数据库操作的批次
        public static final int JDBC_BATCH_SIZE = 1000;
        
        // 最大线程数
        public static final int MAX_THREAD_COUNT = 8;
        
        // 线程池核心线程数
        public static final int CORE_POOL_SIZE = 4;
        
        // 线程池最大线程数
        public static final int MAX_POOL_SIZE = 8;
        
        // 队列容量
        public static final int QUEUE_CAPACITY = 100;
    }
    
    /**
     * 配置批量处理专用线程池
     */
    @Bean("batchProcessingExecutor")
    public Executor batchProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(BatchConstants.CORE_POOL_SIZE);
        executor.setMaxPoolSize(BatchConstants.MAX_POOL_SIZE);
        executor.setQueueCapacity(BatchConstants.QUEUE_CAPACITY);
        executor.setThreadNamePrefix("BatchProcessing-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
