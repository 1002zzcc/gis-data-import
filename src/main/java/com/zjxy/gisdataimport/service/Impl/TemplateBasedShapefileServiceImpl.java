package com.zjxy.gisdataimport.service.Impl;

import com.zjxy.gisdataimport.config.dynamic.DynamicDataSourceHolder;
import com.zjxy.gisdataimport.dto.SysDatabaseDTO;
import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.service.CoordinateTransformService;
import com.zjxy.gisdataimport.service.DataSourceService;
import com.zjxy.gisdataimport.service.GisManageTemplateService;
import com.zjxy.gisdataimport.service.TemplateBasedShapefileService;
import com.zjxy.gisdataimport.shap.ShapefileReader;
import com.zjxy.gisdataimport.util.TemplateFieldMappingUtil;
import lombok.extern.slf4j.Slf4j;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * 基于模板的Shapefile处理服务实现类
 */
@Slf4j
@Service
public class TemplateBasedShapefileServiceImpl implements TemplateBasedShapefileService {

    @Autowired
    private GisManageTemplateService templateService;

    @Autowired
    private ShapefileReader shapefileReader;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private CoordinateTransformService coordinateTransformService;

    @Autowired
    private TemplateFieldMappingUtil fieldMappingUtil;

    @Override
    public Map<String, Object> processShapefileWithTemplate(InputStream zipInputStream, String fileName, Integer templateId) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("开始使用模板处理Shapefile，模板ID: {}, 文件名: {}", templateId, fileName);

            // 获取模板配置
            GisManageTemplate template = templateService.getTemplateById(templateId);
            if (template == null) {
                result.put("success", false);
                result.put("message", "未找到指定的模板，模板ID: " + templateId);
                return result;
            }

            // 使用模板化的Shapefile处理器处理文件
            int featuresProcessed;
            if (shapefileReader instanceof ShapefileReaderImpl) {
                // 调用支持模板的处理方法
                featuresProcessed = ((ShapefileReaderImpl) shapefileReader)
                    .processShapefileZipWithTemplate(zipInputStream, fileName, template);
            } else {
                // 降级到基础处理方法
                featuresProcessed = shapefileReader.processShapefileZip(zipInputStream, fileName);
            }

            // 生成处理报告
            Map<String, Object> report = generateProcessingReport(featuresProcessed, 0, template);

            result.put("success", true);
            result.put("message", "使用模板处理Shapefile成功");
            result.put("templateId", templateId);
            result.put("templateName", template.getNameZh());
            result.put("featuresProcessed", featuresProcessed);
            result.put("report", report);

            log.info("使用模板处理Shapefile完成，处理了 {} 条记录", featuresProcessed);

        } catch (Exception e) {
            log.error("使用模板处理Shapefile失败", e);
            result.put("success", false);
            result.put("message", "处理失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> processShapefileWithTemplateFromPath(String zipFilePath, Integer templateId) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("从路径使用模板处理Shapefile，模板ID: {}, 文件路径: {}", templateId, zipFilePath);

            // 获取模板配置
            GisManageTemplate template = templateService.getTemplateById(templateId);
            if (template == null) {
                result.put("success", false);
                result.put("message", "未找到指定的模板，模板ID: " + templateId);
                return result;
            }

            // 使用模板化的Shapefile处理器处理文件
            int featuresProcessed;
            if (shapefileReader instanceof com.zjxy.gisdataimport.service.Impl.ShapefileReaderImpl) {
                // 对于路径处理，需要先读取文件再调用模板方法
                try (java.io.FileInputStream fis = new java.io.FileInputStream(zipFilePath)) {
                    featuresProcessed = ((com.zjxy.gisdataimport.service.Impl.ShapefileReaderImpl) shapefileReader)
                        .processShapefileZipWithTemplate(fis, new java.io.File(zipFilePath).getName(), template);
                }
            } else {
                // 降级到基础处理方法
                featuresProcessed = shapefileReader.processShapefileZipFromPath(zipFilePath);
            }

            // 生成处理报告
            Map<String, Object> report = generateProcessingReport(featuresProcessed, 0, template);

            result.put("success", true);
            result.put("message", "使用模板处理Shapefile成功");
            result.put("templateId", templateId);
            result.put("templateName", template.getNameZh());
            result.put("featuresProcessed", featuresProcessed);
            result.put("report", report);

            log.info("从路径使用模板处理Shapefile完成，处理了 {} 条记录", featuresProcessed);

        } catch (Exception e) {
            log.error("从路径使用模板处理Shapefile失败", e);
            result.put("success", false);
            result.put("message", "处理失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public GeoFeatureEntity convertFeatureWithTemplate(SimpleFeature feature, SimpleFeatureType schema, GisManageTemplate template) {
        GeoFeatureEntity entity = new GeoFeatureEntity();

        try {
            // 设置要素ID
            entity.setFeatureId(feature.getID());

            // 处理几何信息
            if (feature.getDefaultGeometryProperty() != null && feature.getDefaultGeometryProperty().getValue() != null) {
                String geometryWkt = feature.getDefaultGeometryProperty().getValue().toString();
                String transformedWkt = applyCoordinateTransformWithTemplate(geometryWkt, template);
                entity.setGeometry(transformedWkt);
            }

            // 处理属性信息
            Map<String, Object> attributes = new HashMap<>();
            Map<String, String> fieldMapping = fieldMappingUtil.extractFieldMapping(template);
            Map<String, String> fieldTypeMapping = fieldMappingUtil.extractFieldTypeMapping(template);

            // 遍历Shapefile中的所有字段
            for (int i = 0; i < schema.getAttributeCount(); i++) {
                String shpFieldName = schema.getDescriptor(i).getLocalName();
                Object shpFieldValue = feature.getAttribute(shpFieldName);

                // 检查该字段是否在映射配置中
                if (fieldMapping.containsKey(shpFieldName)) {
                    String dbFieldName = fieldMapping.get(shpFieldName);
                    String dbFieldType = fieldTypeMapping.get(shpFieldName);

                    // 根据目标数据库字段类型进行值转换
                    Object convertedValue = fieldMappingUtil.convertValueToTargetType(
                        shpFieldValue, dbFieldType, shpFieldName);
                    attributes.put(dbFieldName, convertedValue);

                    log.debug("字段映射: {} ({}) -> {} ({}), 原始值: {}, 转换值: {}",
                        shpFieldName,
                        shpFieldValue != null ? shpFieldValue.getClass().getSimpleName() : "null",
                        dbFieldName, dbFieldType, shpFieldValue, convertedValue);
                }
            }

            entity.setAttributes(convertMapToJson(attributes));
            log.debug("成功转换要素: {}, 属性数量: {}", feature.getID(), attributes.size());

        } catch (Exception e) {
            log.error("转换要素失败: {}", e.getMessage(), e);
            throw new RuntimeException("转换要素失败: " + e.getMessage(), e);
        }

        return entity;
    }

    @Override
    public Map<String, Object> validateDataWithTemplate(List<SimpleFeature> features, GisManageTemplate template) {
        Map<String, Object> result = new HashMap<>();

        int validCount = 0;
        int invalidCount = 0;
        List<String> errors = new ArrayList<>();

        for (SimpleFeature feature : features) {
            try {
                // 基础验证逻辑
                if (feature.getDefaultGeometryProperty() != null &&
                    feature.getDefaultGeometryProperty().getValue() != null) {
                    validCount++;
                } else {
                    invalidCount++;
                    errors.add("要素 " + feature.getID() + " 缺少几何信息");
                }
            } catch (Exception e) {
                invalidCount++;
                errors.add("要素 " + feature.getID() + " 验证失败: " + e.getMessage());
            }
        }

        result.put("totalCount", features.size());
        result.put("validCount", validCount);
        result.put("invalidCount", invalidCount);
        result.put("errors", errors);
        result.put("isValid", invalidCount == 0);

        return result;
    }

    @Override
    public Map<String, String> getFieldMappingFromTemplate(GisManageTemplate template) {
        return fieldMappingUtil.extractFieldMapping(template);
    }



    @Override
    public String applyCoordinateTransformWithTemplate(String geometryWkt, GisManageTemplate template) {
        try {
            // 检查模板是否启用坐标转换
            if (template.getIsZh() == null || !template.getIsZh()) {
                log.debug("模板未启用坐标转换，返回原始几何数据");
                return geometryWkt;
            }

            // 获取模板中的坐标系配置
            String sourceCoordSystem = template.getOriginalCoordinateSystem();
            String targetCoordSystem = template.getTargetCoordinateSystem();

            if (sourceCoordSystem == null || targetCoordSystem == null) {
                log.warn("模板中坐标系配置不完整，使用默认转换");
                return coordinateTransformService.transformGeometry(geometryWkt);
            }

            // 使用模板配置进行坐标转换
            log.debug("使用模板进行坐标转换: {} -> {}", sourceCoordSystem, targetCoordSystem);
            return coordinateTransformService.transformGeometryWithCoordSystems(
                geometryWkt, sourceCoordSystem, targetCoordSystem);

        } catch (Exception e) {
            log.warn("模板化坐标转换失败，使用原始几何数据: {}", e.getMessage());
            return geometryWkt;
        }
    }

    @Override
    public Map<String, Object> applyValidationRulesWithTemplate(Map<String, Object> data, GisManageTemplate template) {
        Map<String, Object> result = new HashMap<>();

        // 基础实现：简单验证
        boolean isValid = true;
        List<String> errors = new ArrayList<>();

        // 检查必填字段
        if (!data.containsKey("geometry") || data.get("geometry") == null) {
            isValid = false;
            errors.add("缺少几何信息");
        }

        result.put("isValid", isValid);
        result.put("errors", errors);

        return result;
    }

    @Override
    public Map<String, Object> getTargetTableInfoFromTemplate(GisManageTemplate template) {
        Map<String, Object> tableInfo = new HashMap<>();

        try {
            // 从模板获取目标表信息
            String tableName = template.getTableName();
            String dataBase = template.getDataBase();
            String dataBaseMode = template.getDataBaseMode();

            // 设置表名
            if (tableName != null && !tableName.trim().isEmpty()) {
                tableInfo.put("tableName", tableName);
            } else {
                tableInfo.put("tableName", "geo_features"); // 默认表名
                log.warn("模板中未配置表名，使用默认表名: geo_features");
            }

            // 设置数据库模式
            if (dataBaseMode != null && !dataBaseMode.trim().isEmpty()) {
                tableInfo.put("schema", dataBaseMode);
            } else {
                tableInfo.put("schema", "public"); // 默认模式
            }

            // 设置数据库
            if (dataBase != null && !dataBase.trim().isEmpty()) {
                tableInfo.put("database", dataBase);
            } else {
                log.warn("模板中未配置数据库信息");
            }

            // 设置数据源名称
            String datasourceName = template.getDatasourceName();
            if (datasourceName != null && !datasourceName.trim().isEmpty()) {
                tableInfo.put("datasource", datasourceName);
            } else {
                tableInfo.put("datasource", "master"); // 默认数据源
            }

            log.info("目标表信息: 数据源={}, 数据库={}, 模式={}, 表名={}",
                tableInfo.get("datasource"), tableInfo.get("database"),
                tableInfo.get("schema"), tableInfo.get("tableName"));

        } catch (Exception e) {
            log.error("获取目标表信息失败: {}", e.getMessage(), e);
            // 设置默认值
            tableInfo.put("tableName", "geo_features");
            tableInfo.put("schema", "public");
            tableInfo.put("datasource", "master");
        }

        return tableInfo;
    }

    @Override
    public List<GeoFeatureEntity> batchProcessFeaturesWithTemplate(List<SimpleFeature> features,
                                                                  SimpleFeatureType schema,
                                                                  GisManageTemplate template) {
        List<GeoFeatureEntity> entities = new ArrayList<>();

        for (SimpleFeature feature : features) {
            try {
                GeoFeatureEntity entity = convertFeatureWithTemplate(feature, schema, template);
                entities.add(entity);
            } catch (Exception e) {
                log.warn("转换要素失败，跳过要素 {}: {}", feature.getID(), e.getMessage());
            }
        }

        return entities;
    }

    @Override
    public Map<String, Object> generateProcessingReport(int processedCount, int errorCount, GisManageTemplate template) {
        Map<String, Object> report = new HashMap<>();

        report.put("templateId", template.getId());
        report.put("templateName", template.getNameZh());
        report.put("processedCount", processedCount);
        report.put("errorCount", errorCount);
        report.put("successRate", errorCount == 0 ? 100.0 : (double) processedCount / (processedCount + errorCount) * 100);
        report.put("processTime", new Date());

        return report;
    }

    private String convertMapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":\"")
                .append(entry.getValue().toString().replace("\"", "\\\""))
                .append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}
