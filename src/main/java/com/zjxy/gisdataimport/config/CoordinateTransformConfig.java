package com.zjxy.gisdataimport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 坐标转换配置类
 * 用于配置GIS数据导入过程中的坐标系转换参数
 */
@Configuration
@ConfigurationProperties(prefix = "gis.coordinate.transform")
public class CoordinateTransformConfig {

    /**
     * 是否启用坐标转换
     */
    private boolean enabled = true;

    /**
     * 源坐标系 - Shapefile数据的原始坐标系
     */
    private String sourceCoordSystem = "CGCS2000XY";

    /**
     * 目标坐标系 - 转换后存储到数据库的坐标系
     */
    private String targetCoordSystem = "CGCS2000";

    /**
     * 是否记录转换日志
     */
    private boolean logTransformation = false;

    /**
     * 转换失败时的处理策略
     * - KEEP_ORIGINAL: 保留原始数据
     * - SET_ERROR: 设置为错误标记
     * - SKIP_RECORD: 跳过该记录
     */
    private FailureStrategy failureStrategy = FailureStrategy.KEEP_ORIGINAL;

    public enum FailureStrategy {
        KEEP_ORIGINAL,
        SET_ERROR,
        SKIP_RECORD
    }

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSourceCoordSystem() {
        return sourceCoordSystem;
    }

    public void setSourceCoordSystem(String sourceCoordSystem) {
        this.sourceCoordSystem = sourceCoordSystem;
    }

    public String getTargetCoordSystem() {
        return targetCoordSystem;
    }

    public void setTargetCoordSystem(String targetCoordSystem) {
        this.targetCoordSystem = targetCoordSystem;
    }

    public boolean isLogTransformation() {
        return logTransformation;
    }

    public void setLogTransformation(boolean logTransformation) {
        this.logTransformation = logTransformation;
    }

    public FailureStrategy getFailureStrategy() {
        return failureStrategy;
    }

    public void setFailureStrategy(FailureStrategy failureStrategy) {
        this.failureStrategy = failureStrategy;
    }
}
