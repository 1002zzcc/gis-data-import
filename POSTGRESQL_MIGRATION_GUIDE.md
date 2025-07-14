# 🔄 GIS-Data-Import PostgreSQL数据库迁移指南

## 📋 迁移概述

本指南详细说明如何将 `gis-data-import` 项目从MySQL迁移到PostgreSQL数据库，以与 `gisresourcemanage` 项目保持一致的数据库架构。

## 🎯 迁移目标

### **原架构 → 新架构**

| 组件 | 原架构 (MySQL) | 新架构 (PostgreSQL) |
|------|----------------|---------------------|
| 数据库 | MySQL 8.0 | PostgreSQL 12+ |
| 连接池 | Druid | Druid (保持不变) |
| 数据源 | 单数据源 | 动态多数据源 |
| 坐标系 | 硬编码 | 配置化 |
| JSON支持 | JSON | JSONB (更高效) |

## 🔧 已完成的迁移工作

### 1. **依赖配置更新**

#### 1.1 **数据库驱动替换**
```xml
<!-- 原MySQL驱动 -->
<!-- <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency> -->

<!-- 新PostgreSQL驱动 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.2.27</version>
</dependency>
```

#### 1.2 **动态数据源支持**
```xml
<!-- 动态数据源 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>dynamic-datasource-spring-boot-starter</artifactId>
    <version>3.6.1</version>
</dependency>
```

### 2. **动态数据源架构**

#### 2.1 **核心组件**
- ✅ `DynamicDataSource` - 动态数据源路由
- ✅ `DynamicDataSourceHolder` - 线程级数据源持有者
- ✅ `DynamicDataSourceConfig` - 数据源配置类
- ✅ `DataSourceUtils` - 数据源工具类

#### 2.2 **数据源切换机制**
```java
// 切换到指定数据源
DynamicDataSourceHolder.setDynamicDataSourceKey("resourcemanage");

// 执行数据库操作
// ...

// 清除数据源设置
DynamicDataSourceHolder.removeDynamicDataSourceKey();
```

### 3. **配置文件更新**

#### 3.1 **开发环境配置 (application-dev.yml)**
```yaml
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          url: jdbc:postgresql://192.168.1.250:5438/resourcemanage
          username: root
          password: root
          driver-class-name: org.postgresql.Driver
        slave:
          url: jdbc:postgresql://192.168.1.250:5438/rights
          username: root
          password: root
          driver-class-name: org.postgresql.Driver
```

#### 3.2 **生产环境配置 (application-prod.yml)**
```yaml
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          url: jdbc:postgresql://172.31.221.8:54321/resourcemanage
          username: xych
          password: Xych-123
          driver-class-name: org.postgresql.Driver
```

### 4. **数据库表结构**

#### 4.1 **PostgreSQL建表脚本**
- ✅ `database_setup_postgresql.sql` - 完整的PostgreSQL建表脚本
- ✅ 支持JSONB字段类型
- ✅ 创建必要的索引
- ✅ 添加表和字段注释

#### 4.2 **主要表结构**
```sql
-- GIS管理模板表
CREATE TABLE gis_manage_template (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(255) NOT NULL,
    name_zh VARCHAR(255) NOT NULL,
    type INTEGER,
    is_zh BOOLEAN DEFAULT FALSE,
    map JSONB,  -- 使用JSONB替代JSON
    -- ... 其他字段
);

-- 地理要素数据表
CREATE TABLE geo_features (
    id BIGSERIAL PRIMARY KEY,
    feature_id VARCHAR(255),
    geometry TEXT,
    attributes JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5. **批量插入优化**

#### 5.1 **PostgreSQL优化的批量插入**
```java
// 支持JSONB类型的批量插入
String sql = "INSERT INTO geo_features (feature_id, geometry, attributes, created_at) VALUES (?, ?, ?::jsonb, ?)";

jdbcTemplate.batchUpdate(sql, entities, entities.size(), 
    (PreparedStatement ps, GeoFeatureEntity entity) -> {
        ps.setString(1, entity.getFeatureId());
        ps.setString(2, entity.getGeometry());
        ps.setString(3, entity.getAttributes());  // 自动转换为JSONB
        ps.setTimestamp(4, Timestamp.valueOf(entity.getCreatedAt()));
    });
```

## 🚀 部署和测试步骤

### 1. **数据库准备**

#### 1.1 **创建PostgreSQL数据库**
```sql
-- 连接到PostgreSQL服务器
psql -h 192.168.1.250 -p 5438 -U root

-- 创建数据库
CREATE DATABASE resourcemanage WITH ENCODING 'UTF8';

-- 切换到新数据库
\c resourcemanage;

-- 执行建表脚本
\i database_setup_postgresql.sql;
```

#### 1.2 **验证表创建**
```sql
-- 查看所有表
\dt

-- 查看模板表结构
\d gis_manage_template

-- 查看示例数据
SELECT * FROM gis_manage_template;
```

### 2. **应用配置**

#### 2.1 **环境变量设置**
```bash
# 开发环境
export SPRING_PROFILES_ACTIVE=dev

# 生产环境
export SPRING_PROFILES_ACTIVE=prod
```

#### 2.2 **数据库连接测试**
```java
// 测试数据源连接
@Test
public void testDataSourceConnection() {
    SysDatabaseDTO database = new SysDatabaseDTO();
    database.setNameEn("resourcemanage");
    database.setIp("192.168.1.250");
    database.setPort("5438");
    database.setUsername("root");
    database.setPassword(Base64.getEncoder().encodeToString("root".getBytes()));
    
    Boolean result = dataSourceService.testConnection(database);
    assertTrue(result);
}
```

### 3. **功能验证**

#### 3.1 **模板功能测试**
```bash
# 获取所有模板
curl -X GET http://localhost:8080/api/template-shapefile/templates

# 创建新模板
curl -X POST http://localhost:8080/api/template-shapefile/templates \
  -H "Content-Type: application/json" \
  -d '{
    "nameZh": "PostgreSQL测试模板",
    "tableName": "geo_features",
    "type": 2,
    "isZh": true,
    "dataBase": "resourcemanage"
  }'
```

#### 3.2 **数据导入测试**
```bash
# 使用模板导入Shapefile
curl -X POST http://localhost:8080/api/template-shapefile/upload-with-template \
  -F "file=@test.zip" \
  -F "templateId=1"
```

### 4. **性能对比测试**

#### 4.1 **批量插入性能**
```java
// 测试不同批次大小的性能
@Test
public void testBatchInsertPerformance() {
    List<GeoFeatureEntity> entities = generateTestData(10000);
    
    // 测试不同批次大小
    int[] batchSizes = {500, 1000, 2000, 5000};
    
    for (int batchSize : batchSizes) {
        long startTime = System.currentTimeMillis();
        batchInsertService.optimizedBatchInsert(entities, batchSize);
        long endTime = System.currentTimeMillis();
        
        log.info("批次大小: {}, 耗时: {} ms", batchSize, (endTime - startTime));
    }
}
```

## 📊 性能优化建议

### 1. **PostgreSQL配置优化**

#### 1.1 **连接池配置**
```yaml
druid:
  initial-size: 20
  min-idle: 5
  max-active: 200
  max-wait: 6000000
  validation-query: SELECT 1  # PostgreSQL使用SELECT 1
```

#### 1.2 **PostgreSQL服务器配置**
```conf
# postgresql.conf
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB
maintenance_work_mem = 64MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
```

### 2. **索引优化**

#### 2.1 **JSONB索引**
```sql
-- 为JSONB字段创建GIN索引
CREATE INDEX idx_geo_features_attributes ON geo_features USING GIN(attributes);

-- 为特定JSONB路径创建索引
CREATE INDEX idx_geo_features_name ON geo_features USING GIN((attributes->'name'));
```

#### 2.2 **几何数据索引**
```sql
-- 如果使用PostGIS，创建空间索引
CREATE INDEX idx_geo_features_geom ON geo_features USING GIST(ST_GeomFromText(geometry));
```

## 🔍 故障排除

### 1. **常见问题**

#### 1.1 **连接问题**
```
错误: FATAL: password authentication failed for user "root"
解决: 检查PostgreSQL的pg_hba.conf配置，确保允许密码认证
```

#### 1.2 **JSONB类型问题**
```
错误: column "attributes" is of type jsonb but expression is of type character varying
解决: 在SQL中使用 ?::jsonb 进行类型转换
```

#### 1.3 **字符编码问题**
```
错误: invalid byte sequence for encoding "UTF8"
解决: 确保数据库创建时指定UTF8编码，检查输入数据编码
```

### 2. **性能问题**

#### 2.1 **批量插入慢**
- 检查批次大小设置
- 确认索引是否过多
- 考虑使用COPY命令替代INSERT

#### 2.2 **查询慢**
- 检查是否缺少必要索引
- 分析查询执行计划
- 考虑使用JSONB操作符优化查询

## ✅ 迁移检查清单

### **部署前检查**
- [ ] PostgreSQL服务器已安装并运行
- [ ] 数据库和用户已创建
- [ ] 网络连接正常
- [ ] 防火墙端口已开放

### **应用配置检查**
- [ ] 依赖已更新到PostgreSQL驱动
- [ ] 配置文件已更新数据库连接信息
- [ ] 动态数据源配置正确
- [ ] 环境变量已设置

### **功能测试检查**
- [ ] 数据库连接测试通过
- [ ] 模板CRUD操作正常
- [ ] Shapefile导入功能正常
- [ ] 批量插入性能满足要求
- [ ] 坐标转换功能正常

### **生产部署检查**
- [ ] 数据库备份策略已制定
- [ ] 监控和日志配置完成
- [ ] 性能基准测试完成
- [ ] 回滚方案已准备

## 🎉 迁移完成

恭喜！您已成功将 `gis-data-import` 项目迁移到PostgreSQL数据库。新架构具有以下优势：

1. **统一数据库平台** - 与gisresourcemanage项目保持一致
2. **动态数据源支持** - 支持运行时切换数据库
3. **更好的JSON支持** - JSONB提供更高效的JSON操作
4. **企业级特性** - PostgreSQL提供更多企业级功能
5. **更好的扩展性** - 支持PostGIS等空间数据扩展

现在您可以享受PostgreSQL带来的强大功能和性能优势！🚀
