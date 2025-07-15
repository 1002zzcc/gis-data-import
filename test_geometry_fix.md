# 几何字段修复测试

## 🔧 修复内容

已修复 PostgreSQL 几何字段插入问题：

### 问题
- 错误: 字段 "geom" 的类型为 geometry, 但表达式的类型为 character varying
- 原因: 直接插入 WKT 字符串到 PostgreSQL 的 geometry 字段

### 解决方案
1. **SQL 修改**: 使用 `ST_GeomFromText(?, 4326)` 替代 `?` 占位符
2. **参数处理**: 确保传递正确的 WKT 字符串给几何字段
3. **属性映射**: 优先使用原始属性，回退到解析的 JSON 属性

### 修改的代码

#### SQL 构建
```java
// 之前
sql.append(String.join(", ", Collections.nCopies(dbFields.size(), "?")));

// 现在
for (String field : dbFields) {
    if (field.equals(geometryField)) {
        placeholders.add("ST_GeomFromText(?, 4326)");  // 使用 SRID 4326
    } else {
        placeholders.add("?");
    }
}
```

#### 参数处理
```java
// 之前
if (dbField.equals(geometryField)) {
    params[i] = entity.getGeometry();
}

// 现在
if (dbField.equals(geometryField)) {
    String geometryWkt = entity.getGeometry();
    if (geometryWkt != null && !geometryWkt.trim().isEmpty()) {
        params[i] = geometryWkt;
    } else {
        params[i] = null;  // 如果几何为空，插入 NULL
    }
}
```

## 🧪 测试步骤

### 1. 检查应用状态
```bash
curl -X GET http://localhost:8080/api/remote-db/test-connection
```

### 2. 验证模板配置
```bash
curl -X GET http://localhost:8080/api/remote-db/template/1830/detail
```

### 3. 检查目标表
```bash
curl -X GET http://localhost:8080/api/remote-db/check-table/t_gas_point_cs
```

### 4. 测试数据导入
```bash
curl -X POST http://localhost:8080/api/template-shapefile/process-with-template \
  -F "filePath=/path/to/shapefile.zip" \
  -F "templateId=1830"
```

### 5. 验证数据插入
在远程数据库中执行：
```sql
-- 检查插入的记录数
SELECT COUNT(*) FROM t_gas_point_cs;

-- 查看插入的数据
SELECT id, gjz, x, y, ST_AsText(geom) as geometry_wkt 
FROM t_gas_point_cs 
ORDER BY id DESC 
LIMIT 10;

-- 验证几何数据的有效性
SELECT id, gjz, ST_IsValid(geom) as is_valid_geometry
FROM t_gas_point_cs 
WHERE geom IS NOT NULL
LIMIT 10;
```

## 📊 预期结果

### 成功的日志输出
```
DEBUG - 构建动态SQL成功: INSERT INTO public.t_gas_point_cs (gjz, x, y, geom) VALUES (?, ?, ?, ST_GeomFromText(?, 4326))
INFO  - 成功插入 1000 条记录到目标表: t_gas_point_cs
INFO  - 使用模板处理Shapefile完成，处理了 1000 条记录
```

### 数据库验证
- `SELECT COUNT(*) FROM t_gas_point_cs;` 应该返回新增的记录数
- 几何字段应该包含有效的 PostGIS 几何对象
- `ST_AsText(geom)` 应该返回正确的 WKT 格式几何数据

## 🔍 故障排除

### 如果仍有几何错误
1. 检查 WKT 格式是否正确
2. 验证坐标系 SRID 是否匹配
3. 确认目标表的几何字段类型

### 如果属性映射错误
1. 检查模板的 mapJson 配置
2. 验证字段名映射是否正确
3. 确认数据类型转换

### 如果坐标转换问题
1. 检查模板的坐标系配置
2. 验证坐标转换服务是否正常
3. 确认转换后的坐标格式

## 💡 优化建议

1. **SRID 配置**: 根据实际需求调整 SRID（当前使用 4326）
2. **错误处理**: 增加更详细的几何验证和错误提示
3. **性能优化**: 对于大批量数据，考虑使用 COPY 命令
4. **数据验证**: 插入前验证 WKT 格式的有效性
