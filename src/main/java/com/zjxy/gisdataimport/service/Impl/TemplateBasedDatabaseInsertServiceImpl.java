package com.zjxy.gisdataimport.service.Impl;

import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.service.BatchInsertService;
import com.zjxy.gisdataimport.service.TemplateBasedDatabaseInsertService;
import com.zjxy.gisdataimport.service.TemplateBasedShapefileService;
import com.zjxy.gisdataimport.shap.ShapefileReader;
import com.zjxy.gisdataimport.util.TemplateFieldMappingUtil;
import lombok.extern.slf4j.Slf4j;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 基于模板的数据库插入服务实现类
 */
@Slf4j
@Service
public class TemplateBasedDatabaseInsertServiceImpl implements TemplateBasedDatabaseInsertService {

    @Autowired
    private BatchInsertService batchInsertService;

    @Autowired
    private TemplateBasedShapefileService templateBasedShapefileService;

    @Autowired
    private ShapefileReader shapefileReader;

    @Autowired
    private TemplateFieldMappingUtil fieldMappingUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> batchInsertWithTemplate(List<GeoFeatureEntity> entities, GisManageTemplate template) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取目标表信息
            Map<String, Object> tableInfo = getTargetTableInfo(template);
            String tableName = (String) tableInfo.get("tableName");
            String schema = (String) tableInfo.get("schema");
            String database = (String) tableInfo.get("database");

            log.info("开始批量插入数据到表: {}.{}.{}, 数据量: {}", database, schema, tableName, entities.size());

            // 2. 检查目标表是否存在
            if (isUseCustomTable(template)) {
                boolean tableExists = createTargetTableIfNotExists(template);
                if (!tableExists) {
                    result.put("success", false);
                    result.put("message", "目标表不存在，请先手动执行CREATE TABLE语句创建表。请查看日志获取SQL语句。");
                    result.put("needCreateTable", true);
                    result.put("tableName", tableInfo.get("tableName"));
                    return result;
                }
            }

            // 3. 验证表结构
            Map<String, Object> validationResult = validateTargetTableStructure(template);
            if (!(Boolean) validationResult.get("valid")) {
                result.put("success", false);
                result.put("message", "目标表结构验证失败: " + validationResult.get("message"));
                return result;
            }

            // 4. 根据模板配置决定插入方式
            if (isUseCustomTable(template)) {
                // 使用动态SQL插入到自定义表
                return insertToCustomTable(entities, template);
            } else {
                // 使用默认的geo_features表
                return insertToDefaultTable(entities, template);
            }

        } catch (Exception e) {
            log.error("批量插入数据失败", e);
            result.put("success", false);
            result.put("message", "批量插入失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 插入到默认的geo_features表
     */
    private Map<String, Object> insertToDefaultTable(List<GeoFeatureEntity> entities, GisManageTemplate template) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // 预处理地理要素数据
            batchInsertService.batchPreprocessGeoFeatures(entities);

            // 注意：GeoFeatureEntity 现在是中间态，不直接插入到 geo_features 表
            // 实际的数据库插入应该通过模板配置插入到目标表
            log.warn("BatchInsertService 不再支持直接插入到 geo_features 表，请使用模板化插入");

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            result.put("success", false);
            result.put("insertedCount", 0);
            result.put("processingTime", processingTime);
            result.put("tableName", "geo_features");
            result.put("message", "不再支持插入到默认的 geo_features 表，请配置自定义目标表");
            result.put("note", "数据已预处理但未插入数据库，请使用自定义表配置");

            log.warn("尝试插入到默认表 geo_features，但此功能已禁用。数据已预处理 {} 条记录，耗时: {}ms", entities.size(), processingTime);

        } catch (Exception e) {
            log.error("插入到默认表失败", e);
            result.put("success", false);
            result.put("message", "插入到默认表失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 插入到自定义表
     */
    private Map<String, Object> insertToCustomTable(List<GeoFeatureEntity> entities, GisManageTemplate template) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // 1. 构建动态插入SQL
            Map<String, Object> sqlInfo = buildDynamicInsertSQL(template, entities);
            String insertSQL = (String) sqlInfo.get("sql");
            List<Object[]> batchParams = (List<Object[]>) sqlInfo.get("parameters");

            // 2. 执行批量插入（分批处理）
            log.info("开始执行批量插入 - 记录数: {}, SQL长度: {}", batchParams.size(), insertSQL.length());
            log.debug("执行SQL: {}", insertSQL);

            long insertStartTime = System.currentTimeMillis();
            int successCount = 0;

            try {
                // 设置查询超时时间（60秒）
                jdbcTemplate.setQueryTimeout(60);

                // 分批处理，避免一次性插入过多数据导致超时
                int batchSize = 1000; // 每批1000条记录
                List<List<Object[]>> batches = partitionList(batchParams, batchSize);

                log.info("分批执行插入 - 总批次: {}, 每批大小: {}", batches.size(), batchSize);

                for (int i = 0; i < batches.size(); i++) {
                    List<Object[]> batch = batches.get(i);
                    long batchStartTime = System.currentTimeMillis();

                    log.debug("执行第 {}/{} 批，记录数: {}", i + 1, batches.size(), batch.size());

                    int[] updateCounts = jdbcTemplate.batchUpdate(insertSQL, batch);
                    int batchSuccessCount = Arrays.stream(updateCounts).sum();
                    successCount += batchSuccessCount;

                    long batchEndTime = System.currentTimeMillis();
                    log.info("第 {}/{} 批执行完成 - 成功: {}, 耗时: {}ms",
                            i + 1, batches.size(), batchSuccessCount, batchEndTime - batchStartTime);
                }

                long insertEndTime = System.currentTimeMillis();
                log.info("所有批次插入执行完成，总耗时: {}ms", insertEndTime - insertStartTime);

            } catch (Exception e) {
                long insertEndTime = System.currentTimeMillis();
                log.error("批量插入执行失败，耗时: {}ms, 错误: {}", insertEndTime - insertStartTime, e.getMessage());
                throw e;
            }

            log.info("批量插入结果统计 - 总成功: {}", successCount);

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            result.put("success", true);
            result.put("insertedCount", successCount);
            result.put("processingTime", processingTime);
            result.put("tableName", template.getTableName());
            result.put("sql", insertSQL);
            result.put("message", "成功插入 " + successCount + " 条记录到自定义表");

            log.info("成功插入 {} 条记录到自定义表 {}，耗时: {}ms", successCount, template.getTableName(), processingTime);

        } catch (Exception e) {
            log.error("插入到自定义表失败", e);
            result.put("success", false);
            result.put("message", "插入到自定义表失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> buildDynamicInsertSQL(GisManageTemplate template, List<GeoFeatureEntity> entities) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 获取字段映射
            Map<String, String> fieldMapping = fieldMappingUtil.extractFieldMapping(template);
            Map<String, String> typeMapping = fieldMappingUtil.extractFieldTypeMapping(template);
            Map<String, String> dbTypeMapping = fieldMappingUtil.extractDbFieldTypeMapping(template);

            // 调试日志：输出映射信息
            log.debug("字段映射 (Shapefile -> DB): {}", fieldMapping);
            log.debug("类型映射 (Shapefile -> Type): {}", typeMapping);
            log.debug("数据库字段类型映射 (DB -> Type): {}", dbTypeMapping);

            // 2. 构建表名
            String tableName = template.getTableName();
            if (template.getDataBaseMode() != null && !template.getDataBaseMode().trim().isEmpty()) {
                tableName = template.getDataBaseMode() + "." + tableName;
            }

            // 3. 构建字段列表
            List<String> dbFields = new ArrayList<>(fieldMapping.values());

            // 添加几何字段
            String geometryField = fieldMappingUtil.getGeometryFieldName(template);
            if (!dbFields.contains(geometryField)) {
                dbFields.add(geometryField);
            }

            // 4. 构建INSERT SQL - 特殊处理几何字段
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ").append(tableName).append(" (");
            sql.append(String.join(", ", dbFields));
            sql.append(") VALUES (");

            // 为每个字段构建占位符，几何字段使用 ST_GeomFromText(?)
            List<String> placeholders = new ArrayList<>();
            for (String field : dbFields) {
                if (field.equals(geometryField)) {
                    placeholders.add("ST_GeomFromText(?, 4326)");  // 使用 SRID 4326
                } else {
                    placeholders.add("?");
                }
            }
            sql.append(String.join(", ", placeholders));
            sql.append(")");

            // 5. 构建参数列表
            List<Object[]> batchParams = new ArrayList<>();
            for (GeoFeatureEntity entity : entities) {
                Object[] params = buildParametersForEntity(entity, dbFields, fieldMapping, typeMapping, dbTypeMapping, geometryField);
                batchParams.add(params);
            }

            result.put("sql", sql.toString());
            result.put("parameters", batchParams);
            result.put("fieldCount", dbFields.size());
            result.put("recordCount", entities.size());

            log.debug("构建动态SQL成功: {}", sql.toString());

        } catch (Exception e) {
            log.error("构建动态SQL失败", e);
            throw new RuntimeException("构建动态SQL失败: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 为单个实体构建参数数组
     */
    private Object[] buildParametersForEntity(GeoFeatureEntity entity, List<String> dbFields,
                                            Map<String, String> fieldMapping, Map<String, String> typeMapping,
                                            Map<String, String> dbTypeMapping, String geometryField) {
        Object[] params = new Object[dbFields.size()];
        
        // 解析实体的属性JSON
        Map<String, Object> attributes = parseAttributesJson(entity.getAttributes());
        
        // 创建反向映射：从数据库字段名到Shapefile字段名
        Map<String, String> reverseFieldMapping = new HashMap<>();
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            reverseFieldMapping.put(entry.getValue(), entry.getKey());
        }

        for (int i = 0; i < dbFields.size(); i++) {
            String dbField = dbFields.get(i);

            if (dbField.equals(geometryField)) {
                // 几何字段 - 确保传递 WKT 字符串给 ST_GeomFromText()
                String geometryWkt = entity.getGeometry();
                if (geometryWkt != null && !geometryWkt.trim().isEmpty()) {
                    params[i] = geometryWkt;
                } else {
                    params[i] = null;  // 如果几何为空，插入 NULL
                }
                log.debug("几何字段 {} 设置值: {}", dbField, geometryWkt != null ? geometryWkt.substring(0, Math.min(50, geometryWkt.length())) + "..." : null);
            } else {
                // 属性字段 - 根据字段映射获取对应的Shapefile字段值
                Object value = null;
                String shpFieldName = reverseFieldMapping.get(dbField);

                if (shpFieldName != null && entity.getRawAttributes() != null) {
                    value = entity.getRawAttributes().get(shpFieldName);
                    // 只在TRACE级别记录详细的字段映射日志，避免大量重复输出
                    if (log.isTraceEnabled()) {
                        log.trace("属性字段映射: {} ({}) -> {} ({}), 值: {}",
                            shpFieldName, shpFieldName, dbField, dbField, value);
                    }
                }

                // 如果还是没有找到，尝试直接用数据库字段名查找
                if (value == null && entity.getRawAttributes() != null) {
                    value = entity.getRawAttributes().get(dbField);
                    if (log.isTraceEnabled()) {
                        log.trace("直接字段查找: {} -> 值: {}", dbField, value);
                    }
                }

                // 最后尝试从解析的JSON属性中获取
                if (value == null) {
                    value = attributes.get(dbField);
                    if (log.isTraceEnabled()) {
                        log.trace("JSON属性查找: {} -> 值: {}", dbField, value);
                    }
                }

                // 处理缺失字段：如果字段值为null或空字符串，设置为null
                if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                    if (log.isTraceEnabled()) {
                        log.trace("字段 {} 缺失或为空，设置为 null", dbField);
                    }
                    params[i] = null;
                } else {
                    // 根据目标字段类型进行数据类型转换
                    // 优先使用数据库字段类型映射，如果没有则使用Shapefile字段类型映射
                    String targetType = dbTypeMapping.get(dbField);
                    if (targetType == null || targetType.trim().isEmpty()) {
                        // 如果数据库字段类型映射中没有，尝试从Shapefile字段类型映射中获取
                        // shpFieldName 已在上面定义，直接使用
                        if (shpFieldName != null) {
                            targetType = typeMapping.get(shpFieldName);
                        }
                    }

                    // 调试日志：输出字段转换详情（仅在TRACE级别）
                    if (log.isTraceEnabled()) {
                        log.trace("字段 {} 类型转换详情: dbField={}, shpFieldName={}, targetType={}, originalValue={}",
                                 dbField, dbField, shpFieldName, targetType, value);
                    }

                    Object convertedValue;

                    if (targetType != null && !targetType.trim().isEmpty()) {
                        // 有明确的类型配置，进行类型转换
                        convertedValue = fieldMappingUtil.convertValueToTargetType(value, targetType, dbField);
                        if (log.isTraceEnabled()) {
                            log.trace("字段类型转换: {} -> {} (类型: {}), 原始值: {}, 转换值: {}",
                                     dbField, targetType,
                                     convertedValue != null ? convertedValue.getClass().getSimpleName() : "null",
                                     value, convertedValue);
                        }
                    } else {
                        // 没有类型配置，使用智能推断
                        convertedValue = smartTypeInference(value, dbField);
                        log.debug("字段智能推断: {} (类型: {}), 原始值: {}, 推断值: {}",
                                 dbField,
                                 convertedValue != null ? convertedValue.getClass().getSimpleName() : "null",
                                 value, convertedValue);
                    }

                    // 如果转换后仍然为null，记录日志
                    if (convertedValue == null && log.isDebugEnabled()) {
                        log.debug("字段 {} 转换后为null，设置为 null", dbField);
                    }

                    params[i] = convertedValue;
                }
            }
        }
        
        return params;
    }

    /**
     * 智能类型推断
     * 当模板中没有配置字段类型时，根据值的特征进行推断
     */
    private Object smartTypeInference(Object value, String fieldName) {
        if (value == null) {
            return null;
        }

        String valueStr = value.toString().trim();
        if (valueStr.isEmpty()) {
            return null;
        }

        try {
            // 1. 尝试解析为整数
            if (valueStr.matches("^-?\\d+$")) {
                long longValue = Long.parseLong(valueStr);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue;
                } else {
                    return longValue;
                }
            }

            // 2. 尝试解析为浮点数
            if (valueStr.matches("^-?\\d*\\.\\d+$")) {
                return Double.parseDouble(valueStr);
            }

            // 3. 检查是否为布尔值
            String lowerValue = valueStr.toLowerCase();
            if ("true".equals(lowerValue) || "false".equals(lowerValue) ||
                "1".equals(valueStr) || "0".equals(valueStr) ||
                "yes".equals(lowerValue) || "no".equals(lowerValue)) {
                return "true".equals(lowerValue) || "1".equals(valueStr) || "yes".equals(lowerValue);
            }

            // 4. 检查是否为日期时间格式
            if (isDateTimeFormat(valueStr)) {
                // 对于日期时间，保持字符串格式，让数据库处理
                return valueStr;
            }

            // 5. 默认返回字符串
            return valueStr;

        } catch (Exception e) {
            log.debug("智能类型推断失败，字段: {}, 值: {}, 使用字符串类型", fieldName, valueStr);
            return valueStr;
        }
    }

    /**
     * 检查是否为日期时间格式
     */
    private boolean isDateTimeFormat(String value) {
        // 常见的日期时间格式模式
        String[] patterns = {
            "\\d{4}-\\d{2}-\\d{2}",                    // 2000-01-01
            "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", // 2000-01-01 00:00:00
            "\\d{4}/\\d{2}/\\d{2}",                    // 2000/01/01
            "\\d{2}/\\d{2}/\\d{4}",                    // 01/01/2000
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}" // ISO format
        };

        for (String pattern : patterns) {
            if (value.matches(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 将列表分割为指定大小的批次
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }



    /**
     * 解析属性JSON字符串
     */
    private Map<String, Object> parseAttributesJson(String attributesJson) {
        Map<String, Object> attributes = new HashMap<>();
        
        if (attributesJson == null || attributesJson.trim().isEmpty()) {
            return attributes;
        }
        
        try {
            // 简单的JSON解析（实际项目中建议使用Jackson或Gson）
            attributesJson = attributesJson.trim();
            if (attributesJson.startsWith("{") && attributesJson.endsWith("}")) {
                attributesJson = attributesJson.substring(1, attributesJson.length() - 1);
                String[] pairs = attributesJson.split(",");
                
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replaceAll("\"", "");
                        String value = keyValue[1].trim().replaceAll("\"", "");
                        attributes.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析属性JSON失败: {}", e.getMessage());
        }
        
        return attributes;
    }

    @Override
    public Map<String, Object> getTargetTableInfo(GisManageTemplate template) {
        return templateBasedShapefileService.getTargetTableInfoFromTemplate(template);
    }

    @Override
    public boolean createTargetTableIfNotExists(GisManageTemplate template) {
        try {
            // 1. 检查表是否存在
            String tableName = template.getTableName();
            String schema = template.getDataBaseMode();

            if (tableName == null || tableName.trim().isEmpty()) {
                log.warn("模板中未配置表名，无法创建表");
                return false;
            }

            // 2. 构建完整表名
            String fullTableName = buildFullTableName(schema, tableName);

            // 3. 检查表是否存在
            if (checkTableExists(fullTableName, schema, tableName)) {
                log.info("目标表已存在: {}", fullTableName);
                return true;
            }

            // 4. 表不存在，生成CREATE TABLE语句
            String createTableSQL = generateCreateTableSQL(template);

            // 5. 记录SQL语句并提示手动执行
            log.warn("=== 目标表不存在，请手动执行以下SQL语句创建表 ===");
            log.warn("表名: {}", fullTableName);
            log.warn("SQL语句:");
            log.warn(createTableSQL);
            log.warn("=== 请复制上述SQL语句到数据库中执行 ===");

            // 6. 返回false表示需要手动创建表
            return false;

        } catch (Exception e) {
            log.error("检查/创建目标表失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> validateTargetTableStructure(GisManageTemplate template) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 详细验证模板配置
            List<String> issues = new ArrayList<>();

            // 检查表名
            String tableName = template.getTableName();
            if (tableName == null || tableName.trim().isEmpty()) {
                issues.add("目标表名为空");
            }

            // 检查模板名称
            if (template.getNameZh() == null || template.getNameZh().trim().isEmpty()) {
                issues.add("模板名称为空");
            }

            // 检查字段映射配置
            if (template.getMapJson() == null || template.getMapJson().trim().isEmpty()) {
                issues.add("字段映射配置为空");
            }

            // 检查坐标系配置（如果启用了坐标转换）
            if (template.getIsZh() != null && template.getIsZh()) {
                if (template.getOriginalCoordinateSystem() == null || template.getOriginalCoordinateSystem().trim().isEmpty()) {
                    issues.add("启用了坐标转换但源坐标系为空");
                }
                if (template.getTargetCoordinateSystem() == null || template.getTargetCoordinateSystem().trim().isEmpty()) {
                    issues.add("启用了坐标转换但目标坐标系为空");
                }
            }

            if (issues.isEmpty()) {
                result.put("valid", true);
                result.put("message", "模板配置验证通过");
            } else {
                result.put("valid", false);
                result.put("message", "模板配置验证失败: " + String.join(", ", issues));
                result.put("issues", issues);
                result.put("suggestion", "请检查模板ID " + template.getId() + " 的配置，确保所有必要字段都已正确设置");
            }

        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "模板配置验证失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> transactionalBatchInsert(List<GeoFeatureEntity> entities, GisManageTemplate template) {
        // 在事务中执行批量插入
        return batchInsertWithTemplate(entities, template);
    }

    @Override
    public Map<String, Object> importShapefileWithTemplate(String shapefilePath, GisManageTemplate template) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始使用模板导入Shapefile: {}", shapefilePath);

            // 1. 读取Shapefile
            List<SimpleFeature> features = shapefileReader.readFeatures(shapefilePath);
            SimpleFeatureType schema = shapefileReader.getSchema(shapefilePath);

            // 2. 使用模板转换数据
            List<GeoFeatureEntity> entities = templateBasedShapefileService.batchProcessFeaturesWithTemplate(
                features, schema, template);

            // 3. 插入数据库
            Map<String, Object> insertResult = batchInsertWithTemplate(entities, template);

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            // 4. 生成报告
            result.put("success", insertResult.get("success"));
            result.put("shapefilePath", shapefilePath);
            result.put("templateId", template.getId());
            result.put("templateName", template.getNameZh());
            result.put("totalFeatures", features.size());
            result.put("processedFeatures", entities.size());
            result.put("insertedRecords", insertResult.get("insertedCount"));
            result.put("totalProcessingTime", totalTime);
            result.put("insertResult", insertResult);

            log.info("Shapefile导入完成: 总要素数={}, 处理成功={}, 插入记录={}, 总耗时={}ms", 
                features.size(), entities.size(), insertResult.get("insertedCount"), totalTime);

        } catch (Exception e) {
            log.error("Shapefile导入失败", e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 生成CREATE TABLE SQL语句
     */
    private String generateCreateTableSQL(GisManageTemplate template) {
        try {
            String tableName = template.getTableName();
            String schema = template.getDataBaseMode();

            // 构建完整表名
            String fullTableName = buildFullTableName(schema, tableName);

            // 获取字段映射配置
            List<Map<String, Object>> mapConfig = template.getMap();
            if (mapConfig == null || mapConfig.isEmpty()) {
                throw new RuntimeException("模板中没有字段映射配置");
            }

            StringBuilder sql = new StringBuilder();
            sql.append("CREATE TABLE ").append(fullTableName).append(" (\n");

            // 添加主键ID字段
            sql.append("    id SERIAL PRIMARY KEY,\n");

            // 处理映射字段
            List<String> fieldDefinitions = new ArrayList<>();
            boolean hasGeometryField = false;

            for (Map<String, Object> fieldConfig : mapConfig) {
                Boolean checked = (Boolean) fieldConfig.get("checked");
                if (checked != null && checked) {
                    String fieldName = (String) fieldConfig.get("fieldName");
                    String fieldType = (String) fieldConfig.get("fieldType");
                    String shpFieldType = (String) fieldConfig.get("shpFieldType");

                    if (fieldName != null && !fieldName.trim().isEmpty() &&
                        fieldType != null && !fieldType.trim().isEmpty()) {

                        String columnDefinition = buildColumnDefinition(fieldName, fieldType, shpFieldType);
                        fieldDefinitions.add(columnDefinition);

                        // 检查是否有几何字段
                        if (isGeometryType(fieldType) || isGeometryType(shpFieldType)) {
                            hasGeometryField = true;
                        }
                    }
                }
            }

            // 添加字段定义
            for (int i = 0; i < fieldDefinitions.size(); i++) {
                sql.append("    ").append(fieldDefinitions.get(i));
                if (i < fieldDefinitions.size() - 1) {
                    sql.append(",");
                }
                sql.append("\n");
            }

            sql.append(");\n");

            // 如果有几何字段，添加空间索引
            if (hasGeometryField) {
                String geometryField = getGeometryFieldFromConfig(mapConfig);
                if (geometryField != null) {
                    sql.append("\n-- 创建空间索引\n");
                    sql.append("CREATE INDEX idx_").append(tableName).append("_").append(geometryField)
                       .append(" ON ").append(fullTableName).append(" USING GIST (").append(geometryField).append(");\n");
                }
            }

            // 添加注释
            sql.append("\n-- 表注释\n");
            sql.append("COMMENT ON TABLE ").append(fullTableName).append(" IS '")
               .append("由模板自动生成的表: ").append(template.getNameZh() != null ? template.getNameZh() : tableName)
               .append("';\n");

            return sql.toString();

        } catch (Exception e) {
            log.error("生成CREATE TABLE SQL失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成CREATE TABLE SQL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建列定义
     */
    private String buildColumnDefinition(String fieldName, String fieldType, String shpFieldType) {
        StringBuilder definition = new StringBuilder();
        definition.append(fieldName).append(" ");

        // 根据fieldType确定PostgreSQL数据类型
        String pgType = mapToPostgreSQLType(fieldType, shpFieldType);
        definition.append(pgType);

        // 添加约束
        if (isGeometryType(fieldType) || isGeometryType(shpFieldType)) {
            // 几何字段不添加额外约束
        } else {
            // 非几何字段可以添加默认约束
            if (isStringType(pgType)) {
                // 字符串类型可以为空
            } else if (isNumericType(pgType)) {
                // 数值类型可以为空
            }
        }

        return definition.toString();
    }

    /**
     * 映射到PostgreSQL数据类型
     */
    private String mapToPostgreSQLType(String fieldType, String shpFieldType) {
        if (fieldType == null) {
            fieldType = "";
        }

        String lowerFieldType = fieldType.toLowerCase();

        // 几何类型
        if (isGeometryType(fieldType) || isGeometryType(shpFieldType)) {
            if (shpFieldType != null) {
                String lowerShpType = shpFieldType.toLowerCase();
                if (lowerShpType.contains("point")) {
                    return "GEOMETRY(POINT, 4326)";
                } else if (lowerShpType.contains("line")) {
                    return "GEOMETRY(LINESTRING, 4326)";
                } else if (lowerShpType.contains("polygon")) {
                    return "GEOMETRY(POLYGON, 4326)";
                } else if (lowerShpType.contains("multipoint")) {
                    return "GEOMETRY(MULTIPOINT, 4326)";
                } else if (lowerShpType.contains("multiline")) {
                    return "GEOMETRY(MULTILINESTRING, 4326)";
                } else if (lowerShpType.contains("multipolygon")) {
                    return "GEOMETRY(MULTIPOLYGON, 4326)";
                }
            }
            return "GEOMETRY";
        }

        // 数值类型
        switch (lowerFieldType) {
            case "integer":
            case "int":
            case "int4":
                return "INTEGER";
            case "bigint":
            case "int8":
                return "BIGINT";
            case "numeric":
            case "decimal":
                return "NUMERIC";
            case "double precision":
            case "float8":
                return "DOUBLE PRECISION";
            case "real":
            case "float4":
                return "REAL";
            case "boolean":
            case "bool":
                return "BOOLEAN";
            case "date":
                return "DATE";
            case "timestamp":
            case "timestamp without time zone":
                return "TIMESTAMP";
            case "timestamp with time zone":
                return "TIMESTAMPTZ";
            case "character varying":
            case "varchar":
                return "VARCHAR(255)";
            case "text":
                return "TEXT";
            case "char":
                return "CHAR(1)";
            default:
                // 默认使用VARCHAR
                return "VARCHAR(255)";
        }
    }

    /**
     * 检查是否为几何类型
     */
    private boolean isGeometryType(String type) {
        if (type == null) {
            return false;
        }
        String lowerType = type.toLowerCase();
        return lowerType.contains("point") || lowerType.contains("line") ||
               lowerType.contains("polygon") || lowerType.contains("geometry") ||
               lowerType.contains("multipoint") || lowerType.contains("multiline") ||
               lowerType.contains("multipolygon");
    }

    /**
     * 检查是否为字符串类型
     */
    private boolean isStringType(String pgType) {
        String lowerType = pgType.toLowerCase();
        return lowerType.contains("varchar") || lowerType.contains("text") ||
               lowerType.contains("char");
    }

    /**
     * 检查是否为数值类型
     */
    private boolean isNumericType(String pgType) {
        String lowerType = pgType.toLowerCase();
        return lowerType.contains("integer") || lowerType.contains("bigint") ||
               lowerType.contains("numeric") || lowerType.contains("double") ||
               lowerType.contains("real") || lowerType.contains("decimal");
    }

    /**
     * 从配置中获取几何字段名
     */
    private String getGeometryFieldFromConfig(List<Map<String, Object>> mapConfig) {
        for (Map<String, Object> fieldConfig : mapConfig) {
            Boolean checked = (Boolean) fieldConfig.get("checked");
            if (checked != null && checked) {
                String fieldName = (String) fieldConfig.get("fieldName");
                String fieldType = (String) fieldConfig.get("fieldType");
                String shpFieldType = (String) fieldConfig.get("shpFieldType");

                if (isGeometryType(fieldType) || isGeometryType(shpFieldType)) {
                    return fieldName;
                }
            }
        }
        return null;
    }

    /**
     * 构建完整表名
     */
    private String buildFullTableName(String schema, String tableName) {
        if (schema != null && !schema.trim().isEmpty() && !"public".equals(schema.trim())) {
            return schema + "." + tableName;
        }
        return tableName;
    }

    /**
     * 检查表是否存在
     */
    private boolean checkTableExists(String fullTableName, String schema, String tableName) {
        try {
            String checkSQL;
            if (schema != null && !schema.trim().isEmpty() && !"public".equals(schema.trim())) {
                checkSQL = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
                Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, schema, tableName);
                return count != null && count > 0;
            } else {
                checkSQL = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ? AND table_schema = 'public'";
                Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, tableName);
                return count != null && count > 0;
            }
        } catch (Exception e) {
            log.warn("检查表是否存在时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 判断是否使用自定义表
     */
    private boolean isUseCustomTable(GisManageTemplate template) {
        String tableName = template.getTableName();
        return tableName != null && !tableName.trim().isEmpty() && !"geo_features".equals(tableName);
    }
}
