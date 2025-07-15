package com.zjxy.gisdataimport.entity;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 地理要素实体类 - 中间态数据传输对象
 *
 * 此类用作 Shapefile 数据处理的中间载体，不直接映射到数据库表。
 * 主要用途：
 * 1. 从 Shapefile 读取的原始数据转换
 * 2. 基于模板配置进行字段映射和数据转换
 * 3. 作为动态表插入的数据源
 *
 * 注意：此类已移除数据库映射注解，不会直接插入到 geo_features 表
 */
public class GeoFeatureEntity {

    /**
     * 内部标识ID（仅用于处理过程中的标识，不对应数据库主键）
     */
    private Long id;

    /**
     * 要素唯一标识符（来自 Shapefile 的 Feature ID）
     */
    private String featureId;

    /**
     * 几何信息（WKT 格式的空间数据）
     */
    private String geometry;

    /**
     * 属性信息（JSON 格式存储的业务属性）
     */
    private String attributes;

    /**
     * 处理时间戳（用于跟踪数据处理时间）
     */
    private LocalDateTime createdAt;

    /**
     * 原始属性映射（用于存储从 Shapefile 解析的原始属性）
     */
    private Map<String, Object> rawAttributes;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getRawAttributes() {
        return rawAttributes;
    }

    public void setRawAttributes(Map<String, Object> rawAttributes) {
        this.rawAttributes = rawAttributes;
    }

    /**
     * 获取指定属性值
     * @param attributeName 属性名
     * @return 属性值
     */
    public Object getAttribute(String attributeName) {
        return rawAttributes != null ? rawAttributes.get(attributeName) : null;
    }

    /**
     * 设置属性值
     * @param attributeName 属性名
     * @param value 属性值
     */
    public void setAttribute(String attributeName, Object value) {
        if (rawAttributes == null) {
            rawAttributes = new java.util.HashMap<>();
        }
        rawAttributes.put(attributeName, value);
    }

    /**
     * 检查是否包含指定属性
     * @param attributeName 属性名
     * @return 是否包含
     */
    public boolean hasAttribute(String attributeName) {
        return rawAttributes != null && rawAttributes.containsKey(attributeName);
    }

    @Override
    public String toString() {
        return "GeoFeatureEntity{" +
                "id=" + id +
                ", featureId='" + featureId + '\'' +
                ", geometry='" + (geometry != null ? geometry.substring(0, Math.min(50, geometry.length())) + "..." : null) + '\'' +
                ", attributes='" + attributes + '\'' +
                ", createdAt=" + createdAt +
                ", rawAttributesCount=" + (rawAttributes != null ? rawAttributes.size() : 0) +
                '}';
    }
}
