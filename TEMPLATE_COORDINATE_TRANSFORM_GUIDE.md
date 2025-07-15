# 模板坐标转换功能指南

## 概述

本文档介绍了GIS数据导入系统中基于模板的坐标转换功能。系统根据模板中的 `originalCoordinateSystem`、`targetCoordinateSystem` 和 `isZh` 字段来判断是否进行坐标转换，并执行相应的转换逻辑。

## 模板字段说明

### 1. isZh (Boolean)
- **作用**: 控制是否启用坐标转换
- **取值**: 
  - `true`: 启用坐标转换
  - `false` 或 `null`: 禁用坐标转换
- **默认值**: `false`

### 2. originalCoordinateSystem (String)
- **作用**: 指定源坐标系
- **示例值**: "CGCS2000", "WenZhou2000", "Beijing1954"
- **要求**: 必须是系统支持的坐标系名称

### 3. targetCoordinateSystem (String)
- **作用**: 指定目标坐标系
- **示例值**: "CGCS2000XY", "WenZhouCity", "CGCS2000"
- **要求**: 必须是系统支持的坐标系名称

## 坐标转换逻辑

系统按照以下逻辑判断是否进行坐标转换：

### 1. 基础检查
```java
// 检查几何数据是否有效
if (geometryWkt == null || geometryWkt.trim().isEmpty()) {
    return geometryWkt; // 返回原始数据
}

// 检查是否启用坐标转换
if (template.getIsZh() == null || !template.getIsZh()) {
    return geometryWkt; // 不进行转换
}
```

### 2. 坐标系配置验证
```java
// 验证源坐标系
if (sourceCoordSystem == null || sourceCoordSystem.trim().isEmpty()) {
    return geometryWkt; // 配置不完整，不转换
}

// 验证目标坐标系
if (targetCoordSystem == null || targetCoordSystem.trim().isEmpty()) {
    return geometryWkt; // 配置不完整，不转换
}

// 检查是否为相同坐标系
if (sourceCoordSystem.equals(targetCoordSystem)) {
    return geometryWkt; // 无需转换
}
```

### 3. 坐标系支持检查
```java
// 检查源坐标系是否受支持
if (!coordinateTransformService.isSupportedCoordSystem(sourceCoordSystem)) {
    return geometryWkt; // 不支持，不转换
}

// 检查目标坐标系是否受支持
if (!coordinateTransformService.isSupportedCoordSystem(targetCoordSystem)) {
    return geometryWkt; // 不支持，不转换
}
```

### 4. 执行坐标转换
```java
// 执行转换
String transformedWkt = coordinateTransformService.transformGeometryWithCoordSystems(
    geometryWkt, sourceCoordSystem, targetCoordSystem);

// 检查转换结果
if (transformedWkt == null) {
    return geometryWkt; // 转换失败，返回原始数据
}

return transformedWkt;
```

## 使用示例

### 示例1: 启用坐标转换
```java
GisManageTemplate template = new GisManageTemplate();
template.setIsZh(true);
template.setOriginalCoordinateSystem("CGCS2000");
template.setTargetCoordinateSystem("CGCS2000XY");

String geometryWkt = "POINT(120.5 30.5)";
String result = templateService.applyCoordinateTransformWithTemplate(geometryWkt, template);
// 结果: 转换后的坐标，例如 "POINT(3337641.456 3379401.623)"
```

### 示例2: 禁用坐标转换
```java
GisManageTemplate template = new GisManageTemplate();
template.setIsZh(false); // 或者不设置（默认为false）
template.setOriginalCoordinateSystem("CGCS2000");
template.setTargetCoordinateSystem("CGCS2000XY");

String geometryWkt = "POINT(120.5 30.5)";
String result = templateService.applyCoordinateTransformWithTemplate(geometryWkt, template);
// 结果: "POINT(120.5 30.5)" (原始数据，未转换)
```

### 示例3: 配置不完整
```java
GisManageTemplate template = new GisManageTemplate();
template.setIsZh(true);
template.setOriginalCoordinateSystem(null); // 缺少源坐标系
template.setTargetCoordinateSystem("CGCS2000XY");

String geometryWkt = "POINT(120.5 30.5)";
String result = templateService.applyCoordinateTransformWithTemplate(geometryWkt, template);
// 结果: "POINT(120.5 30.5)" (配置不完整，不转换)
```

## 支持的坐标系

系统支持的坐标系包括但不限于：
- CGCS2000 (中国大地坐标系2000)
- CGCS2000XY (CGCS2000投影坐标系)
- WenZhou2000 (温州2000坐标系)
- WenZhouCity (温州城市坐标系)
- Beijing1954 (北京1954坐标系)

可以通过以下方法查询所有支持的坐标系：
```java
Set<String> supportedCoordSystems = coordinateTransformService.getSupportedCoordSystems();
```

## 错误处理

系统在遇到以下情况时会返回原始几何数据，而不是抛出异常：

1. **几何数据为空或无效**
2. **坐标转换未启用** (`isZh` 为 `false` 或 `null`)
3. **坐标系配置不完整** (源或目标坐标系为空)
4. **坐标系不受支持**
5. **源和目标坐标系相同**
6. **坐标转换过程中发生异常**

所有错误情况都会记录相应的日志信息，便于调试和监控。

## 性能考虑

- 坐标转换是计算密集型操作，建议在必要时才启用
- 系统会检查源和目标坐标系是否相同，避免不必要的转换
- 批量处理时，坐标转换会并行执行以提高性能
- 转换失败时会优雅降级，返回原始数据而不是中断处理流程

## 日志记录

系统会记录以下级别的日志：

- **DEBUG**: 转换过程的详细信息
- **INFO**: 成功的转换操作
- **WARN**: 配置问题或不支持的坐标系
- **ERROR**: 转换过程中的异常

通过调整日志级别可以控制输出的详细程度。
