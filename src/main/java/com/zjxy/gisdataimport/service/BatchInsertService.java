package com.zjxy.gisdataimport.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import com.zjxy.gisdataimport.mapper.GeoFeatureMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 批量插入服务 - 使用MyBatis-Plus优化批量插入性能
 */
@Service
public class BatchInsertService extends ServiceImpl<GeoFeatureMapper, GeoFeatureEntity> {

    /**
     * 批量插入地理要素数据
     * @param entities 要插入的实体列表
     * @return 插入成功的数量
     */
    public int batchInsertGeoFeatures(List<GeoFeatureEntity> entities) {
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

        // 使用MyBatis-Plus的批量插入
        boolean result = this.saveBatch(entities, 1000); // 每批1000条

        return result ? entities.size() : 0;
    }

    /**
     * 高性能批量插入 - 使用原生SQL
     * @param entities 要插入的实体列表
     */
    public void fastBatchInsert(List<GeoFeatureEntity> entities) {
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

        // 分批插入，每批1000条
        final int batchSize = 1000;
        for (int i = 0; i < entities.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, entities.size());
            List<GeoFeatureEntity> batch = entities.subList(i, endIndex);

            // 使用MyBatis-Plus的批量插入
            this.saveBatch(batch);
        }
    }
}
