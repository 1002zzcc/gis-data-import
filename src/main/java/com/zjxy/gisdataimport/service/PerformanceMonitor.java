package com.zjxy.gisdataimport.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控服务 - 监控批量处理性能指标
 */
@Service
public class PerformanceMonitor {
    
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger totalBatches = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private volatile long startTime = 0;
    
    /**
     * 开始监控
     */
    public void startMonitoring() {
        startTime = System.currentTimeMillis();
        totalProcessed.set(0);
        totalBatches.set(0);
        totalProcessingTime.set(0);
        System.out.println("=== 开始性能监控 ===");
    }
    
    /**
     * 记录批次处理完成
     */
    public void recordBatchCompleted(int batchSize, long processingTime) {
        totalProcessed.addAndGet(batchSize);
        totalBatches.incrementAndGet();
        totalProcessingTime.addAndGet(processingTime);
        
        // 每处理1万条记录输出一次进度
        if (totalProcessed.get() % 10000 == 0) {
            printProgress();
        }
    }
    
    /**
     * 输出当前进度
     */
    public void printProgress() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        int processed = totalProcessed.get();
        
        if (processed > 0 && elapsedTime > 0) {
            double recordsPerSecond = (double) processed / (elapsedTime / 1000.0);
            System.out.printf("进度报告 - 已处理: %d 条, 批次数: %d, 耗时: %d ms, 处理速度: %.2f 条/秒%n",
                    processed, totalBatches.get(), elapsedTime, recordsPerSecond);
        }
    }
    
    /**
     * 完成监控并输出最终报告
     */
    public void finishMonitoring() {
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        int processed = totalProcessed.get();
        int batches = totalBatches.get();
        
        System.out.println("=== 性能监控报告 ===");
        System.out.printf("总处理记录数: %d 条%n", processed);
        System.out.printf("总批次数: %d 个%n", batches);
        System.out.printf("总耗时: %d ms (%.2f 秒)%n", totalTime, totalTime / 1000.0);
        
        if (processed > 0 && totalTime > 0) {
            double recordsPerSecond = (double) processed / (totalTime / 1000.0);
            double avgBatchTime = (double) totalProcessingTime.get() / batches;
            double avgBatchSize = (double) processed / batches;
            
            System.out.printf("平均处理速度: %.2f 条/秒%n", recordsPerSecond);
            System.out.printf("平均批次大小: %.0f 条%n", avgBatchSize);
            System.out.printf("平均批次处理时间: %.2f ms%n", avgBatchTime);
        }
        System.out.println("=== 监控结束 ===");
    }
    
    /**
     * 获取当前处理数量
     */
    public int getTotalProcessed() {
        return totalProcessed.get();
    }
    
    /**
     * 获取当前批次数
     */
    public int getTotalBatches() {
        return totalBatches.get();
    }
}
