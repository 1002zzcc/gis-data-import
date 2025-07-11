# GIS数据导入坐标转换功能使用指南

## 📋 功能概述

本项目已集成完整的坐标转换功能，可以在GIS数据导入过程中自动将Shapefile中的几何数据从源坐标系转换为目标坐标系。

## 🚀 主要特性

- ✅ **自动坐标转换**: 在数据导入过程中自动转换几何数据坐标系
- ✅ **多坐标系支持**: 支持CGCS2000、WenZhou2000、Beijing1954等常用坐标系
- ✅ **配置化管理**: 通过配置文件灵活设置转换参数
- ✅ **错误处理**: 完善的异常处理和失败策略
- ✅ **性能优化**: 集成到高性能批量处理流程中
- ✅ **测试接口**: 提供REST API用于测试和验证

## ⚙️ 配置说明

### 1. 基本配置

在 `application.properties` 中配置坐标转换参数：

```properties
# 坐标转换配置
gis.coordinate.transform.enabled=true
gis.coordinate.transform.source-coord-system=CGCS2000XY
gis.coordinate.transform.target-coord-system=CGCS2000
gis.coordinate.transform.log-transformation=false
gis.coordinate.transform.failure-strategy=KEEP_ORIGINAL
```

### 2. 配置参数说明

| 参数 | 说明 | 默认值 | 可选值 |
|------|------|--------|--------|
| `enabled` | 是否启用坐标转换 | `true` | `true/false` |
| `source-coord-system` | 源坐标系 | `CGCS2000XY` | 见支持的坐标系 |
| `target-coord-system` | 目标坐标系 | `CGCS2000` | 见支持的坐标系 |
| `log-transformation` | 是否记录转换日志 | `false` | `true/false` |
| `failure-strategy` | 转换失败策略 | `KEEP_ORIGINAL` | `KEEP_ORIGINAL/SET_ERROR/SKIP_RECORD` |

### 3. 支持的坐标系

| 坐标系名称 | 说明 | 类型 |
|-----------|------|------|
| `CGCS2000` | CGCS2000经纬度坐标 | 地理坐标系 |
| `CGCS2000XY` | CGCS2000投影坐标 | 投影坐标系 |
| `WenZhou2000` | 温州2000坐标系 | 投影坐标系 |
| `WenZhouCity` | 温州城市坐标系 | 投影坐标系 |
| `Beijing1954` | 北京1954坐标系 | 投影坐标系 |

## 🔧 使用方法

### 1. 数据导入时自动转换

当您上传Shapefile文件时，系统会自动根据配置进行坐标转换：

```bash
# 上传Shapefile ZIP文件
curl -X POST "http://localhost:8080/api/shapefiles/upload" \
     -F "file=@your-shapefile.zip"
```

### 2. 测试坐标转换功能

#### 测试单个几何对象转换：

```bash
curl -X POST "http://localhost:8080/api/coordinate/transform" \
     -H "Content-Type: application/json" \
     -d '{
       "wkt": "POINT (499045.9906393343 3096924.4116956145)",
       "sourceCoordSystem": "CGCS2000XY",
       "targetCoordSystem": "CGCS2000"
     }'
```

#### 获取支持的坐标系列表：

```bash
curl -X GET "http://localhost:8080/api/coordinate/coord-systems"
```

#### 快速测试常见几何类型：

```bash
curl -X GET "http://localhost:8080/api/coordinate/test"
```

## 📊 转换示例

### 输入数据示例

```
原始数据 (CGCS2000XY投影坐标):
- POINT (499045.9906393343 3096924.4116956145)
- MULTILINESTRING ((509052.7857999997 3074945.454, 509067.92819999997 3074950.0625))
```

### 转换后数据示例

```
转换后 (CGCS2000经纬度坐标):
- POINT (120.123456 28.123456)
- MULTILINESTRING ((120.234567 27.234567, 120.234568 27.234568))
```

## 🛠️ 自定义配置

### 1. 修改坐标系配置

如果您的数据使用不同的坐标系，可以修改配置：

```properties
# 例如：从温州2000转换为CGCS2000
gis.coordinate.transform.source-coord-system=WenZhou2000
gis.coordinate.transform.target-coord-system=CGCS2000
```

### 2. 添加新的坐标系

在 `CoordSystem.json` 中添加新的坐标系定义：

```json
{
  "YourCustomCoordSystem": {
    "IsBLCoord": false,
    "EllipsoidType": "CGCS2000",
    "L0": 120.0
  }
}
```

在 `PlaneFourParam.json` 中添加转换参数：

```json
{
  "YourCustomCoordSystem-CGCS2000": {
    "dX": 0,
    "dY": 0,
    "K": 0,
    "Alfa": 0,
    "L1": 120.0,
    "L2": 120.0
  }
}
```

## 🚨 注意事项

1. **坐标系匹配**: 确保配置的源坐标系与您的Shapefile数据坐标系一致
2. **转换参数**: 不同坐标系之间的转换需要正确的四参数配置
3. **性能影响**: 坐标转换会增加一定的处理时间，但已优化集成到批量处理流程中
4. **数据验证**: 建议先用小量数据测试转换效果，确认无误后再处理大批量数据

## 🔍 故障排除

### 1. 转换失败

如果坐标转换失败，检查：
- 源坐标系配置是否正确
- 是否存在对应的转换参数
- 输入的WKT格式是否正确

### 2. 性能问题

如果转换影响性能：
- 可以临时禁用转换：`gis.coordinate.transform.enabled=false`
- 调整批量处理参数
- 检查坐标转换算法的复杂度

### 3. 日志调试

启用详细日志：
```properties
gis.coordinate.transform.log-transformation=true
logging.level.com.zjxy.gisdataimport.service.CoordinateTransformService=DEBUG
```

## 📈 性能监控

坐标转换功能已集成到现有的性能监控系统中，您可以在处理过程中看到：

```
=== 性能监控报告 ===
总处理记录数: 300000 条
总批次数: 30 个
总耗时: 45000 ms (45.00 秒)
平均处理速度: 6666.67 条/秒
=== 监控结束 ===
```

转换功能不会显著影响整体处理性能，仍能保持高效的批量处理能力。
