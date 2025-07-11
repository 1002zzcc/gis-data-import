# 坐标转换功能测试指南

## 🧪 快速测试步骤

### 1. 启动应用

```bash
cd gis-data-import
mvn spring-boot:run
```

### 2. 测试坐标转换API

#### 测试您提供的示例数据

**测试点数据：**
```bash
curl -X POST "http://localhost:8080/api/coordinate/transform" \
     -H "Content-Type: application/json" \
     -d '{
       "wkt": "POINT (499045.9906393343 3096924.4116956145)",
       "sourceCoordSystem": "CGCS2000XY",
       "targetCoordSystem": "CGCS2000"
     }'
```

**测试多线数据：**
```bash
curl -X POST "http://localhost:8080/api/coordinate/transform" \
     -H "Content-Type: application/json" \
     -d '{
       "wkt": "MULTILINESTRING ((509052.7857999997 3074945.454, 509067.92819999997 3074950.0625))",
       "sourceCoordSystem": "CGCS2000XY",
       "targetCoordSystem": "CGCS2000"
     }'
```

#### 快速测试所有功能

```bash
curl -X GET "http://localhost:8080/api/coordinate/test"
```

### 3. 检查支持的坐标系

```bash
curl -X GET "http://localhost:8080/api/coordinate/coord-systems"
```

### 4. 测试Shapefile导入

准备一个包含您示例数据类型的Shapefile ZIP文件，然后：

```bash
curl -X POST "http://localhost:8080/api/shapefiles/upload" \
     -F "file=@your-test-shapefile.zip"
```

## 📋 预期结果

### 坐标转换结果示例

**输入 (CGCS2000XY投影坐标):**
```
POINT (499045.9906393343 3096924.4116956145)
```

**输出 (CGCS2000经纬度坐标):**
```
POINT (120.xxx 28.xxx)  // 具体数值取决于转换参数
```

### API响应示例

```json
{
  "success": true,
  "originalWkt": "POINT (499045.9906393343 3096924.4116956145)",
  "transformedWkt": "POINT (120.123456 28.123456)",
  "sourceCoordSystem": "CGCS2000XY",
  "targetCoordSystem": "CGCS2000"
}
```

## 🔧 配置调整

如果需要调整坐标转换配置，修改 `application.properties`：

```properties
# 启用/禁用坐标转换
gis.coordinate.transform.enabled=true

# 设置源坐标系（根据您的Shapefile数据）
gis.coordinate.transform.source-coord-system=CGCS2000XY

# 设置目标坐标系
gis.coordinate.transform.target-coord-system=CGCS2000

# 启用转换日志（调试时使用）
gis.coordinate.transform.log-transformation=true
```

## 🚨 故障排除

### 1. 如果转换失败

检查日志输出，常见问题：
- 坐标系名称拼写错误
- 缺少转换参数配置
- WKT格式不正确

### 2. 如果转换结果不正确

- 确认源坐标系配置是否与实际数据匹配
- 检查转换参数是否正确
- 验证坐标系定义

### 3. 性能问题

- 可以临时禁用转换进行对比测试
- 检查批量处理配置
- 监控内存使用情况

## 📊 性能对比测试

### 测试不同配置下的性能

**1. 禁用坐标转换：**
```properties
gis.coordinate.transform.enabled=false
```

**2. 启用坐标转换：**
```properties
gis.coordinate.transform.enabled=true
```

**3. 对比处理时间和速度**

系统会输出详细的性能报告，您可以对比转换前后的处理速度。

## ✅ 验证清单

- [ ] 应用成功启动
- [ ] 坐标转换API正常响应
- [ ] 测试数据转换结果正确
- [ ] Shapefile导入功能正常
- [ ] 性能满足要求
- [ ] 错误处理机制正常

## 📞 技术支持

如果遇到问题，请检查：

1. **日志输出**: 查看控制台和日志文件
2. **配置文件**: 确认所有配置参数正确
3. **数据格式**: 验证输入数据格式
4. **坐标系支持**: 确认使用的坐标系在支持列表中

完成测试后，您就可以正常使用集成了坐标转换功能的GIS数据导入系统了！
