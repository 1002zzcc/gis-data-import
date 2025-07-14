package com.zjxy.gisdataimport.service;

import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import com.zjxy.gisdataimport.entity.GisManageTemplate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 基于模板的Shapefile处理服务接口
 */
public interface TemplateBasedShapefileService {

    /**
     * 使用模板处理Shapefile ZIP文件
     * @param zipInputStream ZIP文件输入流
     * @param fileName 文件名
     * @param templateId 模板ID
     * @return 处理结果
     */
    Map<String, Object> processShapefileWithTemplate(InputStream zipInputStream, String fileName, Integer templateId);

    /**
     * 使用模板处理Shapefile ZIP文件（从路径）
     * @param zipFilePath ZIP文件路径
     * @param templateId 模板ID
     * @return 处理结果
     */
    Map<String, Object> processShapefileWithTemplateFromPath(String zipFilePath, Integer templateId);

    /**
     * 根据模板转换SimpleFeature为GeoFeatureEntity
     * @param feature SimpleFeature对象
     * @param schema SimpleFeatureType对象
     * @param template 模板配置
     * @return 转换后的实体对象
     */
    GeoFeatureEntity convertFeatureWithTemplate(SimpleFeature feature, SimpleFeatureType schema, GisManageTemplate template);

    /**
     * 根据模板验证数据
     * @param features 要验证的要素列表
     * @param template 模板配置
     * @return 验证结果
     */
    Map<String, Object> validateDataWithTemplate(List<SimpleFeature> features, GisManageTemplate template);

    /**
     * 根据模板获取字段映射关系
     * @param template 模板配置
     * @return 字段映射关系
     */
    Map<String, String> getFieldMappingFromTemplate(GisManageTemplate template);

    /**
     * 根据模板应用坐标转换
     * @param geometryWkt 几何数据WKT字符串
     * @param template 模板配置
     * @return 转换后的WKT字符串
     */
    String applyCoordinateTransformWithTemplate(String geometryWkt, GisManageTemplate template);

    /**
     * 根据模板应用数据验证规则
     * @param data 要验证的数据
     * @param template 模板配置
     * @return 验证结果
     */
    Map<String, Object> applyValidationRulesWithTemplate(Map<String, Object> data, GisManageTemplate template);

    /**
     * 根据模板获取目标数据库表信息
     * @param template 模板配置
     * @return 表信息
     */
    Map<String, Object> getTargetTableInfoFromTemplate(GisManageTemplate template);

    /**
     * 批量处理要素数据（基于模板）
     * @param features 要素列表
     * @param template 模板配置
     * @return 处理后的实体列表
     */
    List<GeoFeatureEntity> batchProcessFeaturesWithTemplate(List<SimpleFeature> features, 
                                                           SimpleFeatureType schema, 
                                                           GisManageTemplate template);

    /**
     * 根据模板生成处理报告
     * @param processedCount 处理的记录数
     * @param errorCount 错误记录数
     * @param template 模板配置
     * @return 处理报告
     */
    Map<String, Object> generateProcessingReport(int processedCount, int errorCount, GisManageTemplate template);
}
