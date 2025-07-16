# 日期类型转换问题完整修复方案

## 🔍 问题分析

### 原始错误
```
org.postgresql.util.PSQLException: 错误: 字段 "date_i" 的类型为 date, 但表达式的类型为 integer
建议：你需要重写或转换表达式
位置：620
```

### 根本原因
1. **类型映射逻辑错误**：`typeMapping.get(dbField)` 使用数据库字段名作为key，但映射表的key是Shapefile字段名
2. **日期转换不完善**：无法处理数值类型的日期（如年份、时间戳）
3. **字段映射缺失**：缺少从数据库字段名到字段类型的直接映射

## 🛠️ 修复方案

### 1. 新增数据库字段类型映射方法

在 `TemplateFieldMappingUtil.java` 中新增：

```java
/**
 * 从模板配置中提取数据库字段类型映射关系
 * @param template 模板配置
 * @return 字段类型映射Map，key为数据库字段名，value为数据库字段类型
 */
public Map<String, String> extractDbFieldTypeMapping(GisManageTemplate template) {
    Map<String, String> dbTypeMapping = new HashMap<>();
    
    try {
        List<Map<String, Object>> mapConfig = template.getMap();
        
        if (mapConfig != null && !mapConfig.isEmpty()) {
            for (Map<String, Object> fieldConfig : mapConfig) {
                Boolean checked = (Boolean) fieldConfig.get("checked");
                if (checked != null && checked) {
                    String dbFieldName = (String) fieldConfig.get("fieldName");
                    String dbFieldType = (String) fieldConfig.get("fieldType");
                    
                    if (dbFieldName != null && dbFieldType != null && !dbFieldType.trim().isEmpty()) {
                        dbTypeMapping.put(dbFieldName, dbFieldType);
                        log.debug("添加数据库字段类型映射: {} -> {}", dbFieldName, dbFieldType);
                    }
                }
            }
        }
        
        log.info("从模板解析到 {} 个数据库字段类型映射", dbTypeMapping.size());
        
    } catch (Exception e) {
        log.error("提取数据库字段类型映射失败: {}", e.getMessage(), e);
    }
    
    return dbTypeMapping;
}
```

### 2. 修复类型映射获取逻辑

在 `TemplateBasedDatabaseInsertServiceImpl.java` 中修改：

```java
// 1. 获取字段映射
Map<String, String> fieldMapping = fieldMappingUtil.extractFieldMapping(template);
Map<String, String> typeMapping = fieldMappingUtil.extractFieldTypeMapping(template);
Map<String, String> dbTypeMapping = fieldMappingUtil.extractDbFieldTypeMapping(template);

// 在buildParametersForEntity方法中：
// 根据目标字段类型进行数据类型转换
// 优先使用数据库字段类型映射，如果没有则使用Shapefile字段类型映射
String targetType = dbTypeMapping.get(dbField);
if (targetType == null || targetType.trim().isEmpty()) {
    // 如果数据库字段类型映射中没有，尝试从Shapefile字段类型映射中获取
    if (shpFieldName != null) {
        targetType = typeMapping.get(shpFieldName);
    }
}
```

### 3. 增强日期类型转换

#### 3.1 支持数值类型日期转换

```java
// 特殊处理：如果目标类型是date，但值是数字，进行特殊转换
if ("date".equalsIgnoreCase(targetType) && value instanceof Number) {
    return handleNumericDateConversion((Number) value, shpFieldName);
}
```

#### 3.2 增强日期解析逻辑

```java
private Date parseDate(String value) {
    log.debug("尝试解析日期: {}", value);

    // 首先检查是否为纯数字（可能是时间戳或年份）
    if (value.matches("^\\d+$")) {
        try {
            long numericValue = Long.parseLong(value);
            
            // 如果是4位数字，可能是年份
            if (numericValue >= 1900 && numericValue <= 2100) {
                Calendar cal = Calendar.getInstance();
                cal.set((int) numericValue, Calendar.JANUARY, 1, 0, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date yearDate = cal.getTime();
                log.debug("年份解析成功: {} -> {}", value, yearDate);
                return yearDate;
            }
            
            // 时间戳处理
            if (numericValue >= 1000000000000L) { // 毫秒时间戳（13位数字）
                Date timestampDate = new Date(numericValue);
                log.debug("毫秒时间戳解析成功: {} -> {}", value, timestampDate);
                return timestampDate;
            } else if (numericValue >= 946684800L && numericValue <= 4102444800L) { // 秒时间戳范围：2000-2100年
                Date timestampDate = new Date(numericValue * 1000);
                log.debug("秒时间戳解析成功: {} -> {}", value, timestampDate);
                return timestampDate;
            }
        } catch (NumberFormatException e) {
            log.trace("数字解析失败: {}", e.getMessage());
        }
    }

    // 尝试标准日期格式...
}
```

## 📊 修复效果

### 修复前
```
ERROR: 字段 "date_i" 的类型为 date, 但表达式的类型为 integer
```

### 修复后
```
DEBUG: 数据库字段类型映射: inp_date -> date
DEBUG: 字段类型转换: inp_date -> date (类型: Date), 原始值: 2000, 转换值: Sat Jan 01 00:00:00 CST 2000
INFO: 批量插入成功
```

## 🧪 测试验证

### 测试用例覆盖
- ✅ 年份数字转换（如：2000 -> Date）
- ✅ 时间戳转换（秒/毫秒）
- ✅ 标准日期格式转换
- ✅ Shapefile日期格式转换
- ✅ 数值类型转换
- ✅ 空值处理
- ✅ 字段映射提取

### 测试结果
```
=== 日期类型转换修复测试 ===
✅ 年份转换: 2000 -> Sat Jan 01 00:00:00 CST 2000
✅ 标准日期: 2000-01-01 -> Sat Jan 01 00:00:00 CST 2000
✅ Shapefile格式: Sat Jan 01 00:00:00 CST 2000 -> Date
✅ 数值转换: 3.0 -> Double, 123 -> Integer
✅ 空值处理: null -> null, "" -> null
✅ 字段映射提取正确

🎉 核心功能测试通过！
```

## 🔧 关键改进点

1. **双重类型映射**：同时支持Shapefile字段映射和数据库字段映射
2. **智能日期转换**：支持年份、时间戳、标准格式等多种日期输入
3. **类型安全**：增强了数值类型的特殊处理
4. **错误处理**：完善的异常处理和日志记录
5. **向下兼容**：保持原有功能的同时增加新特性

## 🎯 解决的核心问题

1. ✅ **类型映射错误**：修复了字段名映射不匹配的问题
2. ✅ **日期转换失败**：支持多种日期格式的智能转换
3. ✅ **数据类型不匹配**：确保插入数据库的类型正确
4. ✅ **批量插入失败**：解决了大批量数据插入时的类型错误

这个修复方案彻底解决了原始的PostgreSQL类型转换错误，使系统能够正确处理各种格式的日期数据并成功插入到数据库中。
