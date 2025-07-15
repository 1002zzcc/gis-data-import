package com.zjxy.gisdataimport.service;

import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import com.zjxy.gisdataimport.entity.GisManageTemplate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.List;
import java.util.Map;

/**
 * 基于模板的数据库插入服务接口
 * 负责将转换后的Shapefile数据插入到指定的数据库表中
 */
public interface TemplateBasedDatabaseInsertService {

    /**
     * 根据模板配置批量插入数据到目标表
     * @param entities 转换后的实体列表
     * @param template 模板配置
     * @return 插入结果统计
     */
    Map<String, Object> batchInsertWithTemplate(List<GeoFeatureEntity> entities, GisManageTemplate template);

    /**
     * 根据模板配置动态创建目标表（如果不存在）
     * @param template 模板配置
     * @return 创建结果
     */
    boolean createTargetTableIfNotExists(GisManageTemplate template);

    /**
     * 根据模板配置获取目标表的完整信息
     * @param template 模板配置
     * @return 表信息
     */
    Map<String, Object> getTargetTableInfo(GisManageTemplate template);

    /**
     * 验证目标表结构是否与模板配置匹配
     * @param template 模板配置
     * @return 验证结果
     */
    Map<String, Object> validateTargetTableStructure(GisManageTemplate template);

    /**
     * 根据模板配置构建动态插入SQL
     * @param template 模板配置
     * @param entities 要插入的数据
     * @return SQL语句和参数
     */
    Map<String, Object> buildDynamicInsertSQL(GisManageTemplate template, List<GeoFeatureEntity> entities);

    /**
     * 完整的Shapefile导入流程（从文件到数据库）
     * @param shapefilePath Shapefile文件路径
     * @param template 模板配置
     * @return 导入结果报告
     */
    Map<String, Object> importShapefileWithTemplate(String shapefilePath, GisManageTemplate template);

    /**
     * 事务性批量插入（支持回滚）
     * @param entities 实体列表
     * @param template 模板配置
     * @return 插入结果
     */
    Map<String, Object> transactionalBatchInsert(List<GeoFeatureEntity> entities, GisManageTemplate template);
}
