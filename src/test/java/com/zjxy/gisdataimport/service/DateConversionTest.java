package com.zjxy.gisdataimport.service;

import com.zjxy.gisdataimport.util.TemplateFieldMappingUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

/**
 * 日期转换测试
 */
@SpringBootTest
public class DateConversionTest {

    @Autowired
    private TemplateFieldMappingUtil fieldMappingUtil;

    @Test
    public void testDateConversion() {
        try {
            System.out.println("=== 日期转换测试 ===");
            
            // 测试用例：模拟实际的日期格式
            Object[][] testCases = {
                {"Sat Jan 01 00:00:00 CST 2000", "date"},           // Shapefile常见格式
                {"2000-01-01", "date"},                             // 标准ISO格式
                {"2000/01/01", "date"},                             // 斜杠格式
                {"01/01/2000", "date"},                             // 美式格式
                {"2000-01-01 00:00:00", "timestamp"},               // 时间戳格式
                {"Jan 01, 2000", "date"},                           // 英文月份格式
                {"01-Jan-2000", "date"},                            // 短英文月份格式
                {"2000-01-01T00:00:00", "timestamp"},               // ISO时间戳
                {"", "date"},                                       // 空字符串
                {null, "date"}                                      // null值
            };
            
            boolean allTestsPassed = true;
            
            for (Object[] testCase : testCases) {
                String dateStr = (String) testCase[0];
                String targetType = (String) testCase[1];
                
                System.out.println("\n--- 测试日期: '" + dateStr + "' → " + targetType + " ---");
                
                try {
                    Object result = fieldMappingUtil.convertValueToTargetType(dateStr, targetType, "test_date_field");
                    
                    if (dateStr == null || dateStr.trim().isEmpty()) {
                        if (result == null) {
                            System.out.println("✅ 空值处理正确: " + result);
                        } else {
                            System.out.println("❌ 空值处理错误，期望null，实际: " + result);
                            allTestsPassed = false;
                        }
                    } else {
                        if (result instanceof Date) {
                            Date dateResult = (Date) result;
                            System.out.println("✅ 转换成功: " + dateResult + " (类型: Date)");
                        } else {
                            System.out.println("❌ 转换失败，期望Date类型，实际: " + 
                                (result != null ? result.getClass().getSimpleName() : "null") + ", 值: " + result);
                            allTestsPassed = false;
                        }
                    }
                    
                } catch (Exception e) {
                    System.out.println("❌ 转换异常: " + e.getMessage());
                    allTestsPassed = false;
                }
            }
            
            if (allTestsPassed) {
                System.out.println("\n🎉 所有日期转换测试通过！");
            } else {
                System.out.println("\n❌ 部分日期转换测试失败");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testSpecificShapefileDateFormat() {
        try {
            System.out.println("\n=== Shapefile日期格式专项测试 ===");
            
            // 模拟实际错误中的日期格式
            String shapefileDateStr = "Sat Jan 01 00:00:00 CST 2000";
            String targetType = "date";
            
            System.out.println("测试Shapefile日期格式: " + shapefileDateStr);
            System.out.println("目标类型: " + targetType);
            
            Object result = fieldMappingUtil.convertValueToTargetType(shapefileDateStr, targetType, "inp_date");
            
            if (result instanceof Date) {
                Date dateResult = (Date) result;
                System.out.println("✅ Shapefile日期转换成功!");
                System.out.println("原始值: " + shapefileDateStr);
                System.out.println("转换结果: " + dateResult);
                System.out.println("结果类型: " + result.getClass().getSimpleName());
                
                // 验证年份
                @SuppressWarnings("deprecation")
                int year = dateResult.getYear() + 1900;
                if (year == 2000) {
                    System.out.println("✅ 年份验证正确: " + year);
                } else {
                    System.out.println("❌ 年份验证失败，期望2000，实际: " + year);
                }
                
            } else {
                System.out.println("❌ Shapefile日期转换失败");
                System.out.println("期望类型: Date");
                System.out.println("实际类型: " + (result != null ? result.getClass().getSimpleName() : "null"));
                System.out.println("实际值: " + result);
            }
            
        } catch (Exception e) {
            System.out.println("❌ Shapefile日期转换测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testDateFormatPatterns() {
        try {
            System.out.println("\n=== 日期格式模式测试 ===");
            
            // 测试各种日期格式模式
            String[] dateFormats = {
                "EEE MMM dd HH:mm:ss zzz yyyy",  // Sat Jan 01 00:00:00 CST 2000
                "yyyy-MM-dd",                    // 2000-01-01
                "yyyy/MM/dd",                    // 2000/01/01
                "MM/dd/yyyy",                    // 01/01/2000
                "dd/MM/yyyy",                    // 01/01/2000
                "yyyy-MM-dd HH:mm:ss",           // 2000-01-01 00:00:00
                "MMM dd, yyyy",                  // Jan 01, 2000
                "dd-MMM-yyyy"                    // 01-Jan-2000
            };
            
            String[] testDates = {
                "Sat Jan 01 00:00:00 CST 2000",
                "2000-01-01",
                "2000/01/01", 
                "01/01/2000",
                "01/01/2000",
                "2000-01-01 00:00:00",
                "Jan 01, 2000",
                "01-Jan-2000"
            };
            
            System.out.println("支持的日期格式模式:");
            for (int i = 0; i < dateFormats.length; i++) {
                System.out.printf("%-35s → %s%n", dateFormats[i], testDates[i]);
            }
            
            System.out.println("\n测试关键格式:");
            String keyFormat = "Sat Jan 01 00:00:00 CST 2000";
            Object result = fieldMappingUtil.convertValueToTargetType(keyFormat, "date", "test_field");
            
            if (result instanceof Date) {
                System.out.println("✅ 关键格式转换成功: " + result);
            } else {
                System.out.println("❌ 关键格式转换失败: " + result);
            }
            
        } catch (Exception e) {
            System.out.println("❌ 日期格式模式测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
