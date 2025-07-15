# 模板配置检查

## 问题分析

从日志中看到模板ID 1830的信息：
```
模板ID: 1830
模板名称: 
源坐标系: 
目标坐标系: 
坐标转换: 禁用
```

所有配置都是空的，这表明模板配置有问题。

## 可能的原因

1. **模板不存在**: 数据库中没有ID为1830的模板
2. **模板配置不完整**: 模板存在但关键字段为空
3. **数据库连接问题**: 无法正确读取模板数据

## 检查步骤

### 1. 检查模板是否存在
```sql
SELECT * FROM gis_manage_template WHERE id = 1830;
```

### 2. 检查模板的关键字段
```sql
SELECT 
    id, 
    table_name, 
    name_zh, 
    original_coordinate_system, 
    target_coordinate_system, 
    is_zh,
    map
FROM gis_manage_template 
WHERE id = 1830;
```

### 3. 检查所有可用的模板
```sql
SELECT id, table_name, name_zh, template_type, in_or_out 
FROM gis_manage_template 
WHERE in_or_out = 'in' 
ORDER BY id;
```

## 解决方案

### 方案1: 使用现有的模板
如果模板1830不存在，可以使用其他模板ID，比如：
```bash
curl -X POST http://localhost:8080/api/template-shapefile/process-with-template \
  -F "filePath=/path/to/shapefile.zip" \
  -F "templateId=1"  # 使用其他存在的模板ID
```

### 方案2: 创建新的模板
如果需要创建新模板，可以插入一个完整的模板配置：

```sql
INSERT INTO gis_manage_template (
    table_name, 
    name_zh, 
    name_en, 
    type, 
    is_zh, 
    original_coordinate_system, 
    target_coordinate_system, 
    template_type, 
    data_base, 
    in_or_out, 
    map
) VALUES (
    't_gas_point_cs',  -- 目标表名
    '燃气点位测试模板', 
    'Gas Point Test Template', 
    2,  -- 点要素
    1,  -- 启用坐标转换
    'CGCS2000', 
    'CGCS2000XY', 
    'shp', 
    'gisdb', 
    'in',
    JSON_ARRAY(
        JSON_OBJECT(
            'shpFieldName', 'gjz',
            'fieldName', 'gjz',
            'fieldType', 'VARCHAR',
            'shpFieldType', 'String',
            'checked', true,
            'description', '关键字'
        ),
        JSON_OBJECT(
            'shpFieldName', 'x',
            'fieldName', 'x',
            'fieldType', 'DOUBLE',
            'shpFieldType', 'Double',
            'checked', true,
            'description', '经度'
        ),
        JSON_OBJECT(
            'shpFieldName', 'y',
            'fieldName', 'y',
            'fieldType', 'DOUBLE',
            'shpFieldType', 'Double',
            'checked', true,
            'description', '纬度'
        ),
        JSON_OBJECT(
            'shpFieldName', 'the_geom',
            'fieldName', 'geom',
            'fieldType', 'GEOMETRY',
            'shpFieldType', 'Geometry',
            'checked', true,
            'coordinateTransform', true,
            'description', '空间坐标'
        )
    )
);
```

### 方案3: 修复现有模板
如果模板1830存在但配置不完整，可以更新它：

```sql
UPDATE gis_manage_template 
SET 
    table_name = 't_gas_point_cs',
    name_zh = '燃气点位测试模板',
    original_coordinate_system = 'CGCS2000',
    target_coordinate_system = 'CGCS2000XY',
    is_zh = 1,
    map = JSON_ARRAY(
        JSON_OBJECT(
            'shpFieldName', 'gjz',
            'fieldName', 'gjz',
            'fieldType', 'VARCHAR',
            'checked', true
        ),
        JSON_OBJECT(
            'shpFieldName', 'the_geom',
            'fieldName', 'geom',
            'fieldType', 'GEOMETRY',
            'checked', true,
            'coordinateTransform', true
        )
    )
WHERE id = 1830;
```

## 验证

配置完成后，重新运行导入：
```bash
curl -X POST http://localhost:8080/api/template-shapefile/process-with-template \
  -F "filePath=/path/to/shapefile.zip" \
  -F "templateId=1830"
```

检查目标表中的数据：
```sql
SELECT * FROM t_gas_point_cs LIMIT 10;
```
