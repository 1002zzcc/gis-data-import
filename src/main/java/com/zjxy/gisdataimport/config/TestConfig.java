package com.zjxy.gisdataimport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 测试配置类 - 用于性能测试参数配置
 */
@Configuration
@ConfigurationProperties(prefix = "test.performance")
public class TestConfig {
    
    /**
     * 是否启用测试模式 (限制处理数据量)
     */
    private boolean enabled = true;
    
    /**
     * 测试数据量限制
     */
    private int maxRecords = 10000;
    
    /**
     * 使用的处理方法
     * - "original": 原始方法
     * - "optimized": 优化方法
     */
    private String method = "optimized";
    
    /**
     * 是否启用详细日志
     */
    private boolean verboseLogging = true;
    
    /**
     * 是否清空数据库表 (测试前)
     */
    private boolean clearTableBeforeTest = false;
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getMaxRecords() {
        return maxRecords;
    }
    
    public void setMaxRecords(int maxRecords) {
        this.maxRecords = maxRecords;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public boolean isVerboseLogging() {
        return verboseLogging;
    }
    
    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }
    
    public boolean isClearTableBeforeTest() {
        return clearTableBeforeTest;
    }
    
    public void setClearTableBeforeTest(boolean clearTableBeforeTest) {
        this.clearTableBeforeTest = clearTableBeforeTest;
    }
}
