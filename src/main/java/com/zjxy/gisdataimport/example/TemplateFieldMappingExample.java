package com.zjxy.gisdataimport.example;

import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.util.TemplateFieldMappingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 模板字段映射使用示例
 * 展示如何根据模板配置进行Shapefile字段到数据库字段的映射和类型转换
 */
@Slf4j
@Component
public class TemplateFieldMappingExample {

    @Autowired
    private TemplateFieldMappingUtil fieldMappingUtil;

    /**
     * 演示字段映射和类型转换的完整流程
     */
    public void demonstrateFieldMapping(GisManageTemplate template) {
        log.info("=== 开始演示模板字段映射 ===");
        
        // 1. 提取字段映射关系
        Map<String, String> fieldMapping = fieldMappingUtil.extractFieldMapping(template);
        log.info("字段映射关系:");
        fieldMapping.forEach((shpField, dbField) -> 
            log.info("  {} -> {}", shpField, dbField));
        
        // 2. 提取字段类型映射关系
        Map<String, String> typeMapping = fieldMappingUtil.extractFieldTypeMapping(template);
        log.info("字段类型映射关系:");
        typeMapping.forEach((shpField, dbType) -> 
            log.info("  {} -> {}", shpField, dbType));
        
        // 3. 模拟Shapefile数据转换
        simulateShapefileDataConversion(fieldMapping, typeMapping);
        
        log.info("=== 模板字段映射演示完成 ===");
    }

    /**
     * 模拟Shapefile数据转换过程
     */
    private void simulateShapefileDataConversion(Map<String, String> fieldMapping, 
                                               Map<String, String> typeMapping) {
        log.info("=== 模拟Shapefile数据转换 ===");
        
        // 模拟从Shapefile读取的原始数据
        Map<String, Object> shapefileData = createMockShapefileData();
        
        // 转换后的数据库数据
        Map<String, Object> databaseData = new HashMap<>();
        
        // 根据映射配置转换数据
        for (Map.Entry<String, Object> entry : shapefileData.entrySet()) {
            String shpFieldName = entry.getKey();
            Object shpFieldValue = entry.getValue();
            
            // 检查是否有字段映射
            if (fieldMapping.containsKey(shpFieldName)) {
                String dbFieldName = fieldMapping.get(shpFieldName);
                String dbFieldType = typeMapping.get(shpFieldName);
                
                // 进行类型转换
                Object convertedValue = fieldMappingUtil.convertValueToTargetType(
                    shpFieldValue, dbFieldType, shpFieldName);
                
                databaseData.put(dbFieldName, convertedValue);
                
                log.info("转换: {} ({}) -> {} ({}) | 原始值: {} -> 转换值: {}", 
                    shpFieldName, 
                    shpFieldValue != null ? shpFieldValue.getClass().getSimpleName() : "null",
                    dbFieldName, dbFieldType, shpFieldValue, convertedValue);
            } else {
                log.debug("跳过未映射字段: {}", shpFieldName);
            }
        }
        
        log.info("转换结果:");
        databaseData.forEach((dbField, value) -> 
            log.info("  {} = {} ({})", dbField, value, 
                value != null ? value.getClass().getSimpleName() : "null"));
    }

    /**
     * 创建模拟的Shapefile数据
     */
    private Map<String, Object> createMockShapefileData() {
        Map<String, Object> data = new HashMap<>();
        
        // 几何字段
        data.put("the_geom", "POINT(120.123456 30.654321)");
        
        // 数值字段
        data.put("GJZ", "123.45");
        data.put("X", 120.123456);
        data.put("Y", 30.654321);
        data.put("DEPTH", "15.5");
        data.put("ELEV", 25.8);
        
        // 字符串字段
        data.put("ID", "P001");
        data.put("PROJECT", "测试项目");
        data.put("TYPE_", "管点");
        data.put("LOCATION", "测试位置");
        data.put("ROADNAME", "测试路名");
        data.put("MATERIAL", "钢管");
        data.put("DIAMETER", "DN300");
        
        // 日期字段
        data.put("DATE_I", "2024-01-15");
        data.put("INP_DATE", "2024-01-15 10:30:00");
        
        // 布尔字段
        data.put("STATUS", "true");
        
        // 空值字段
        data.put("MEMO", null);
        data.put("AUTHOR", "");
        
        return data;
    }

    /**
     * 演示特殊类型转换场景
     */
    public void demonstrateSpecialTypeConversions() {
        log.info("=== 演示特殊类型转换场景 ===");
        
        // 测试各种数据类型转换
        testTypeConversion("123", "integer", "整数转换");
        testTypeConversion("123.45", "double precision", "双精度浮点数转换");
        testTypeConversion("123.45", "integer", "浮点数转整数转换");
        testTypeConversion("true", "boolean", "布尔值转换");
        testTypeConversion("2024-01-15", "date", "日期转换");
        testTypeConversion("2024-01-15 10:30:00", "timestamp", "时间戳转换");
        testTypeConversion("测试文本", "character varying", "字符串转换");
        testTypeConversion("", "character varying", "空字符串转换");
        testTypeConversion(null, "integer", "空值转换");
        
        // 测试错误转换
        testTypeConversion("abc", "integer", "错误的整数转换");
        testTypeConversion("xyz", "double precision", "错误的浮点数转换");
        
        log.info("=== 特殊类型转换演示完成 ===");
    }

    /**
     * 测试单个类型转换
     */
    private void testTypeConversion(Object value, String targetType, String description) {
        try {
            Object result = fieldMappingUtil.convertValueToTargetType(value, targetType, "test_field");
            log.info("{}: {} ({}) -> {} ({})", 
                description, value, 
                value != null ? value.getClass().getSimpleName() : "null",
                result, 
                result != null ? result.getClass().getSimpleName() : "null");
        } catch (Exception e) {
            log.warn("{}: 转换失败 - {}", description, e.getMessage());
        }
    }

    /**
     * 演示几何字段处理
     */
    public void demonstrateGeometryFieldHandling(GisManageTemplate template) {
        log.info("=== 演示几何字段处理 ===");
        
        String geometryFieldName = fieldMappingUtil.getGeometryFieldName(template);
        log.info("几何字段名: {}", geometryFieldName);
        
        // 模拟几何数据处理
        String wktGeometry = "POINT(120.123456 30.654321)";
        log.info("原始几何数据: {}", wktGeometry);
        
        // 这里可以添加坐标转换等处理
        log.info("处理后几何数据将存储到字段: {}", geometryFieldName);
        
        log.info("=== 几何字段处理演示完成 ===");
    }
}
