package com.zjxy.gisdataimport.util;

import com.zjxy.gisdataimport.entity.GisManageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 模板字段映射工具类
 * 用于处理Shapefile字段到数据库字段的映射和类型转换
 */
@Slf4j
@Component
public class TemplateFieldMappingUtil {

    /**
     * 常用日期格式
     */
    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd",
        "yyyy/MM/dd", 
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy/MM/dd HH:mm:ss"
    };

    /**
     * 从模板配置中提取字段映射关系
     * @param template 模板配置
     * @return 字段映射Map，key为Shapefile字段名，value为数据库字段名
     */
    public Map<String, String> extractFieldMapping(GisManageTemplate template) {
        Map<String, String> mapping = new HashMap<>();
        
        try {
            List<Map<String, Object>> mapConfig = template.getMap();
            
            if (mapConfig != null && !mapConfig.isEmpty()) {
                for (Map<String, Object> fieldConfig : mapConfig) {
                    Boolean checked = (Boolean) fieldConfig.get("checked");
                    if (checked != null && checked) {
                        String shpFieldName = (String) fieldConfig.get("shpFieldName");
                        String dbFieldName = (String) fieldConfig.get("fieldName");
                        
                        if (isValidFieldMapping(shpFieldName, dbFieldName)) {
                            mapping.put(shpFieldName, dbFieldName);
                            log.debug("添加字段映射: {} -> {}", shpFieldName, dbFieldName);
                        }
                    }
                }
            }
            
            log.info("从模板解析到 {} 个有效字段映射", mapping.size());
            
        } catch (Exception e) {
            log.error("提取字段映射失败: {}", e.getMessage(), e);
        }
        
        return mapping;
    }

    /**
     * 从模板配置中提取字段类型映射关系
     * @param template 模板配置
     * @return 字段类型映射Map，key为Shapefile字段名，value为数据库字段类型
     */
    public Map<String, String> extractFieldTypeMapping(GisManageTemplate template) {
        Map<String, String> typeMapping = new HashMap<>();
        
        try {
            List<Map<String, Object>> mapConfig = template.getMap();
            
            if (mapConfig != null && !mapConfig.isEmpty()) {
                for (Map<String, Object> fieldConfig : mapConfig) {
                    Boolean checked = (Boolean) fieldConfig.get("checked");
                    if (checked != null && checked) {
                        String shpFieldName = (String) fieldConfig.get("shpFieldName");
                        String shpFieldType = (String) fieldConfig.get("shpFieldType");
                        String dbFieldType = (String) fieldConfig.get("fieldType");
                        
                        if (shpFieldName != null && dbFieldType != null && !dbFieldType.trim().isEmpty()) {
                            typeMapping.put(shpFieldName, dbFieldType);
                            log.debug("添加类型映射: {} ({}) -> {}", shpFieldName, shpFieldType, dbFieldType);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("提取字段类型映射失败: {}", e.getMessage(), e);
        }
        
        return typeMapping;
    }

    /**
     * 根据目标数据库字段类型转换值
     * @param value 原始值
     * @param targetType 目标数据库字段类型
     * @param shpFieldName Shapefile字段名（用于日志）
     * @return 转换后的值
     */
    public Object convertValueToTargetType(Object value, String targetType, String shpFieldName) {
        if (value == null) {
            return null;
        }

        try {
            String valueStr = value.toString().trim();
            
            // 处理空字符串
            if (valueStr.isEmpty()) {
                return null;
            }

            // 根据目标类型进行转换
            return performTypeConversion(valueStr, targetType);
            
        } catch (Exception e) {
            log.warn("字段 {} 的值类型转换失败: {} -> {}, 错误: {}, 使用原始值", 
                shpFieldName, value, targetType, e.getMessage());
            return value;
        }
    }

    /**
     * 执行具体的类型转换
     */
    private Object performTypeConversion(String valueStr, String targetType) {
        switch (targetType.toLowerCase()) {
            case "integer":
            case "int":
            case "int4":
                return parseInteger(valueStr);
                
            case "bigint":
            case "int8":
                return parseLong(valueStr);
                
            case "numeric":
            case "decimal":
                return parseBigDecimal(valueStr);
                
            case "double precision":
            case "float8":
                return parseDouble(valueStr);
                
            case "real":
            case "float4":
                return parseFloat(valueStr);
                
            case "boolean":
            case "bool":
                return parseBoolean(valueStr);
                
            case "date":
                return parseDate(valueStr);
                
            case "timestamp":
            case "timestamp without time zone":
            case "timestamp with time zone":
                return parseTimestamp(valueStr);
                
            case "character varying":
            case "varchar":
            case "text":
            case "char":
            default:
                return valueStr;
        }
    }

    /**
     * 解析整数
     */
    private Integer parseInteger(String value) {
        try {
            // 处理浮点数字符串，先转为double再转为int
            if (value.contains(".")) {
                return (int) Double.parseDouble(value);
            }
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法转换为整数: " + value);
        }
    }

    /**
     * 解析长整数
     */
    private Long parseLong(String value) {
        try {
            if (value.contains(".")) {
                return (long) Double.parseDouble(value);
            }
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法转换为长整数: " + value);
        }
    }

    /**
     * 解析BigDecimal
     */
    private BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法转换为数值: " + value);
        }
    }

    /**
     * 解析双精度浮点数
     */
    private Double parseDouble(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法转换为双精度浮点数: " + value);
        }
    }

    /**
     * 解析单精度浮点数
     */
    private Float parseFloat(String value) {
        try {
            return Float.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法转换为单精度浮点数: " + value);
        }
    }

    /**
     * 解析布尔值
     */
    private Boolean parseBoolean(String value) {
        String lowerValue = value.toLowerCase();
        if ("true".equals(lowerValue) || "1".equals(lowerValue) || "yes".equals(lowerValue) || "y".equals(lowerValue)) {
            return true;
        } else if ("false".equals(lowerValue) || "0".equals(lowerValue) || "no".equals(lowerValue) || "n".equals(lowerValue)) {
            return false;
        } else {
            throw new IllegalArgumentException("无法转换为布尔值: " + value);
        }
    }

    /**
     * 解析日期
     */
    private Date parseDate(String value) {
        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false);
                return sdf.parse(value);
            } catch (ParseException ignored) {
                // 继续尝试下一个格式
            }
        }
        throw new IllegalArgumentException("无法解析日期格式: " + value);
    }

    /**
     * 解析时间戳
     */
    private Date parseTimestamp(String value) {
        // 时间戳解析逻辑与日期相同，但可以扩展支持更多格式
        return parseDate(value);
    }

    /**
     * 验证字段映射是否有效
     */
    private boolean isValidFieldMapping(String shpFieldName, String dbFieldName) {
        return shpFieldName != null && !shpFieldName.trim().isEmpty() &&
               dbFieldName != null && !dbFieldName.trim().isEmpty();
    }

    /**
     * 获取几何字段映射
     * @param template 模板配置
     * @return 几何字段名，如果没有配置则返回默认值
     */
    public String getGeometryFieldName(GisManageTemplate template) {
        try {
            List<Map<String, Object>> mapConfig = template.getMap();
            
            if (mapConfig != null && !mapConfig.isEmpty()) {
                for (Map<String, Object> fieldConfig : mapConfig) {
                    Boolean checked = (Boolean) fieldConfig.get("checked");
                    String shpFieldType = (String) fieldConfig.get("shpFieldType");
                    String dbFieldName = (String) fieldConfig.get("fieldName");
                    
                    if (checked != null && checked && 
                        shpFieldType != null && isGeometryType(shpFieldType) &&
                        dbFieldName != null && !dbFieldName.trim().isEmpty()) {
                        return dbFieldName;
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取几何字段名失败: {}", e.getMessage(), e);
        }
        
        return "geom"; // 默认几何字段名
    }

    /**
     * 判断是否为几何类型
     */
    private boolean isGeometryType(String fieldType) {
        if (fieldType == null) {
            return false;
        }
        
        String lowerType = fieldType.toLowerCase();
        return lowerType.contains("point") || 
               lowerType.contains("line") || 
               lowerType.contains("polygon") || 
               lowerType.contains("geometry") ||
               lowerType.contains("multipoint") ||
               lowerType.contains("multiline") ||
               lowerType.contains("multipolygon");
    }
}
