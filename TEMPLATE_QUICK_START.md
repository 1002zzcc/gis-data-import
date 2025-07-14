# 🚀 基于模板的Shapefile导入 - 快速开始指南

## 📋 前置准备

### 1. **数据库设置**
```sql
-- 在MySQL中执行以下命令创建数据库和表
source database_setup.sql;
```

### 2. **启动应用**
```bash
mvn spring-boot:run
```

### 3. **访问测试页面**
打开浏览器访问：http://localhost:8080/template-upload.html

## 🎯 快速测试步骤

### 步骤1：创建测试模板

访问测试页面后：

1. 点击 **"创建新模板"** 按钮
2. 填写模板信息：
   - **模板中文名称**: `测试点要素模板`
   - **模板英文名称**: `Test Point Feature Template`
   - **目标表名**: `geo_features`
   - **几何类型**: 选择 `点表`
   - **是否坐标转换**: 选择 `是`
   - **源坐标系**: `CGCS2000`
   - **目标坐标系**: `CGCS2000XY`

3. **字段映射配置** (复制粘贴以下JSON)：
```json
[
  {
    "shpFieldName": "NAME",
    "fieldName": "feature_name",
    "dataType": "String",
    "required": true,
    "description": "要素名称"
  },
  {
    "shpFieldName": "TYPE",
    "fieldName": "feature_type",
    "dataType": "String",
    "required": false,
    "description": "要素类型"
  },
  {
    "shpFieldName": "the_geom",
    "fieldName": "geometry",
    "dataType": "Geometry",
    "required": true,
    "coordinateTransform": true,
    "description": "几何数据"
  }
]
```

4. 点击 **"创建模板"** 按钮

### 步骤2：测试模板导入

1. 点击 **"刷新模板列表"** 加载刚创建的模板
2. 从列表中选择刚创建的模板
3. 选择一个Shapefile ZIP文件
4. 点击 **"使用模板上传并处理"**

## 🔧 API测试

### 1. **获取所有模板**
```bash
curl -X GET http://localhost:8080/api/template-shapefile/templates
```

### 2. **创建模板**
```bash
curl -X POST http://localhost:8080/api/template-shapefile/templates \
  -H "Content-Type: application/json" \
  -d '{
    "nameZh": "API测试模板",
    "nameEn": "API Test Template",
    "tableName": "geo_features",
    "type": 2,
    "isZh": true,
    "originalCoordinateSystem": "CGCS2000",
    "targetCoordinateSystem": "CGCS2000XY",
    "templateType": "shp",
    "dataBase": "gisdb",
    "inOrOut": "in",
    "map": [
      {
        "shpFieldName": "NAME",
        "fieldName": "feature_name",
        "dataType": "String",
        "required": true
      }
    ]
  }'
```

### 3. **使用模板上传文件**
```bash
curl -X POST http://localhost:8080/api/template-shapefile/upload-with-template \
  -F "file=@/path/to/your/shapefile.zip" \
  -F "templateId=1"
```

### 4. **从路径处理文件**
```bash
curl -X POST http://localhost:8080/api/template-shapefile/process-with-template \
  -F "filePath=/path/to/your/shapefile.zip" \
  -F "templateId=1"
```

## 📊 验证结果

### 1. **检查数据库**
```sql
-- 查看模板数据
SELECT * FROM gis_manage_template;

-- 查看导入的地理要素数据
SELECT id, feature_id, LEFT(geometry, 100) as geometry_preview, 
       LEFT(attributes, 200) as attributes_preview, created_at 
FROM geo_features 
ORDER BY created_at DESC 
LIMIT 10;

-- 统计导入数据量
SELECT COUNT(*) as total_features FROM geo_features;
```

### 2. **检查日志**
查看应用日志中的处理信息：
- 模板加载日志
- 坐标转换日志
- 批量插入性能日志

## 🎨 模板配置示例

### 点要素模板
```json
{
  "nameZh": "城市POI点模板",
  "type": 2,
  "isZh": true,
  "originalCoordinateSystem": "CGCS2000",
  "targetCoordinateSystem": "CGCS2000XY",
  "map": [
    {
      "shpFieldName": "NAME",
      "fieldName": "poi_name",
      "dataType": "String",
      "required": true
    },
    {
      "shpFieldName": "ADDRESS",
      "fieldName": "address",
      "dataType": "String",
      "required": false
    },
    {
      "shpFieldName": "the_geom",
      "fieldName": "geometry",
      "dataType": "Geometry",
      "required": true,
      "coordinateTransform": true
    }
  ]
}
```

### 线要素模板
```json
{
  "nameZh": "道路网络模板",
  "type": 3,
  "isZh": true,
  "originalCoordinateSystem": "CGCS2000",
  "targetCoordinateSystem": "WenZhou2000",
  "map": [
    {
      "shpFieldName": "ROAD_NAME",
      "fieldName": "road_name",
      "dataType": "String",
      "required": true
    },
    {
      "shpFieldName": "ROAD_TYPE",
      "fieldName": "road_type",
      "dataType": "String",
      "required": true
    },
    {
      "shpFieldName": "ROAD_LEVEL",
      "fieldName": "road_level",
      "dataType": "Integer",
      "required": false
    },
    {
      "shpFieldName": "the_geom",
      "fieldName": "geometry",
      "dataType": "Geometry",
      "required": true,
      "coordinateTransform": true
    }
  ]
}
```

## 🔍 故障排除

### 常见问题

1. **模板创建失败**
   - 检查数据库连接
   - 确认JSON格式正确
   - 查看应用日志

2. **文件上传失败**
   - 确认文件是ZIP格式
   - 检查文件大小限制
   - 确认模板已选择

3. **坐标转换失败**
   - 检查坐标系配置文件
   - 确认源坐标系和目标坐标系正确
   - 查看转换日志

4. **数据插入失败**
   - 检查目标表是否存在
   - 确认字段映射正确
   - 查看数据库日志

### 调试技巧

1. **启用详细日志**
```properties
logging.level.com.zjxy.gisdataimport=DEBUG
```

2. **检查模板配置**
```bash
curl -X GET http://localhost:8080/api/template-shapefile/templates/1
```

3. **验证模板配置**
```bash
curl -X POST http://localhost:8080/api/template-shapefile/templates/validate \
  -H "Content-Type: application/json" \
  -d '{"模板JSON配置"}'
```

## 🎉 成功标志

如果一切正常，您应该看到：

1. ✅ 模板创建成功
2. ✅ 文件上传成功
3. ✅ 坐标转换正常
4. ✅ 数据插入成功
5. ✅ 性能监控正常

恭喜！您已经成功配置并测试了基于模板的Shapefile数据导入系统！🎊
