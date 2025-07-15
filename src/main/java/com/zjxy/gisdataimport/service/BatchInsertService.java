package com.zjxy.gisdataimport.service;

import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 批量数据处理服务
 *
 * 注意：此服务不再直接操作 geo_features 表，而是提供数据预处理功能
 * GeoFeatureEntity 现在作为中间态数据传输对象使用
 */
@Slf4j
@Service
public class BatchInsertService {

    /**
     * 预处理地理要素数据（设置时间戳等）
     * @param entities 要处理的实体列表
     * @return 处理成功的数量
     */
    public int preprocessGeoFeatures(List<GeoFeatureEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        // 设置创建时间
        LocalDateTime now = LocalDateTime.now();
        entities.forEach(entity -> {
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(now);
            }
        });

        log.debug("预处理了 {} 条地理要素数据", entities.size());
        return entities.size();
    }

    /**
     * 批量预处理地理要素数据
     * @param entities 要处理的实体列表
     */
    public void batchPreprocessGeoFeatures(List<GeoFeatureEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        // 设置创建时间
        LocalDateTime now = LocalDateTime.now();
        entities.forEach(entity -> {
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(now);
            }
        });

        log.debug("批量预处理了 {} 条地理要素数据", entities.size());
    }

    /**
     * 验证地理要素数据的完整性
     * @param entities 要验证的实体列表
     * @return 验证结果
     */
    public boolean validateGeoFeatures(List<GeoFeatureEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }

        for (GeoFeatureEntity entity : entities) {
            if (entity.getFeatureId() == null || entity.getFeatureId().trim().isEmpty()) {
                log.warn("发现无效的地理要素：featureId 为空");
                return false;
            }
        }

        return true;
    }

    /**
     * 统计地理要素数据信息
     * @param entities 要统计的实体列表
     * @return 统计信息
     */
    public String getStatistics(List<GeoFeatureEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return "无数据";
        }

        long withGeometry = entities.stream().filter(e -> e.getGeometry() != null).count();
        long withAttributes = entities.stream().filter(e -> e.getAttributes() != null).count();
        long withRawAttributes = entities.stream().filter(e -> e.getRawAttributes() != null && !e.getRawAttributes().isEmpty()).count();

        return String.format("总数: %d, 含几何: %d, 含属性: %d, 含原始属性: %d",
                           entities.size(), withGeometry, withAttributes, withRawAttributes);
    }
}
