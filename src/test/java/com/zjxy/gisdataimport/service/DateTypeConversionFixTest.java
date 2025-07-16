package com.zjxy.gisdataimport.service;

import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.service.Impl.TemplateBasedDatabaseInsertServiceImpl;
import com.zjxy.gisdataimport.util.TemplateFieldMappingUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

/**
 * 日期类型转换修复测试
 */
@SpringBootTest
public class DateTypeConversionFixTest {

    @Autowired
    private TemplateFieldMappingUtil fieldMappingUtil;

    @Test
    public void testDateTypeConversionFix() {
        try {
            System.out.println("=== 日期类型转换修复测试 ===");
            
            // 测试用例：模拟实际的日期转换场景
            Object[][] testCases = {
                // 原始值, 目标类型, 期望结果类型
                {"2000", "date", Date.class},                           // 年份数字字符串
                {2000, "date", Date.class},                             // 年份数字
                {"946684800", "date", Date.class},                      // 秒时间戳字符串
                {946684800L, "date", Date.class},                       // 秒时间戳
                {"946684800000", "date", Date.class},                   // 毫秒时间戳字符串
                {946684800000L, "date", Date.class},                    // 毫秒时间戳
                {"2000-01-01", "date", Date.class},                     // 标准日期格式
                {"Sat Jan 01 00:00:00 CST 2000", "date", Date.class},   // Shapefile日期格式
                {"3.0", "double precision", Double.class},              // 浮点数
                {"123", "integer", Integer.class},                      // 整数
                {null, "date", null},                                   // null值
                {"", "date", null}                                      // 空字符串
            };
            
            boolean allTestsPassed = true;
            
            for (Object[] testCase : testCases) {
                Object originalValue = testCase[0];
                String targetType = (String) testCase[1];
                Class<?> expectedType = (Class<?>) testCase[2];
                
                System.out.println("\n--- 测试用例 ---");
                System.out.println("原始值: " + originalValue + " (类型: " + 
                    (originalValue != null ? originalValue.getClass().getSimpleName() : "null") + ")");
                System.out.println("目标类型: " + targetType);
                System.out.println("期望结果类型: " + (expectedType != null ? expectedType.getSimpleName() : "null"));
                
                try {
                    Object result = fieldMappingUtil.convertValueToTargetType(originalValue, targetType, "test_field");
                    
                    if (expectedType == null) {
                        if (result == null) {
                            System.out.println("✅ 空值处理正确");
                        } else {
                            System.out.println("❌ 空值处理错误，期望null，实际: " + result);
                            allTestsPassed = false;
                        }
                    } else {
                        if (result != null && expectedType.isInstance(result)) {
                            System.out.println("✅ 转换成功: " + result + " (类型: " + result.getClass().getSimpleName() + ")");
                            
                            // 特殊验证日期类型
                            if (result instanceof Date && originalValue != null) {
                                Date dateResult = (Date) result;
                                System.out.println("   日期详情: " + dateResult);
                                
                                // 验证年份转换
                                if ("2000".equals(originalValue.toString()) || Integer.valueOf(2000).equals(originalValue)) {
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(dateResult);
                                    int year = cal.get(Calendar.YEAR);
                                    if (year == 2000) {
                                        System.out.println("   ✅ 年份验证正确: " + year);
                                    } else {
                                        System.out.println("   ❌ 年份验证失败，期望2000，实际: " + year);
                                        allTestsPassed = false;
                                    }
                                }
                            }
                        } else {
                            System.out.println("❌ 转换失败，期望类型: " + expectedType.getSimpleName() + 
                                ", 实际类型: " + (result != null ? result.getClass().getSimpleName() : "null") + 
                                ", 值: " + result);
                            allTestsPassed = false;
                        }
                    }
                    
                } catch (Exception e) {
                    System.out.println("❌ 转换异常: " + e.getMessage());
                    allTestsPassed = false;
                }
            }
            
            System.out.println("\n=== 测试结果 ===");
            if (allTestsPassed) {
                System.out.println("🎉 所有日期类型转换测试通过！");
            } else {
                System.out.println("❌ 部分日期类型转换测试失败");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testDbFieldTypeMappingExtraction() {
        try {
            System.out.println("\n=== 数据库字段类型映射提取测试 ===");
            
            // 创建模拟模板
            GisManageTemplate template = new GisManageTemplate();
            
            // 创建字段映射配置
            List<Map<String, Object>> mapConfig = new ArrayList<>();
            
            // 添加日期字段配置
            Map<String, Object> dateFieldConfig = new HashMap<>();
            dateFieldConfig.put("checked", true);
            dateFieldConfig.put("shpFieldName", "inp_date_shp");
            dateFieldConfig.put("fieldName", "inp_date");
            dateFieldConfig.put("fieldType", "date");
            dateFieldConfig.put("shpFieldType", "String");
            mapConfig.add(dateFieldConfig);
            
            // 添加数值字段配置
            Map<String, Object> numericFieldConfig = new HashMap<>();
            numericFieldConfig.put("checked", true);
            numericFieldConfig.put("shpFieldName", "depth_shp");
            numericFieldConfig.put("fieldName", "depth");
            numericFieldConfig.put("fieldType", "double precision");
            numericFieldConfig.put("shpFieldType", "Double");
            mapConfig.add(numericFieldConfig);
            
            template.setMap(mapConfig);
            
            // 测试字段映射提取
            Map<String, String> fieldMapping = fieldMappingUtil.extractFieldMapping(template);
            Map<String, String> typeMapping = fieldMappingUtil.extractFieldTypeMapping(template);
            Map<String, String> dbTypeMapping = fieldMappingUtil.extractDbFieldTypeMapping(template);
            
            System.out.println("字段映射 (Shapefile -> DB): " + fieldMapping);
            System.out.println("类型映射 (Shapefile -> Type): " + typeMapping);
            System.out.println("数据库字段类型映射 (DB -> Type): " + dbTypeMapping);
            
            // 验证映射结果
            boolean mappingCorrect = true;
            
            if (!fieldMapping.containsKey("inp_date_shp") || !"inp_date".equals(fieldMapping.get("inp_date_shp"))) {
                System.out.println("❌ 字段映射错误");
                mappingCorrect = false;
            }
            
            if (!dbTypeMapping.containsKey("inp_date") || !"date".equals(dbTypeMapping.get("inp_date"))) {
                System.out.println("❌ 数据库字段类型映射错误");
                mappingCorrect = false;
            }
            
            if (mappingCorrect) {
                System.out.println("✅ 字段映射提取正确");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 字段映射提取测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
