# GeoFeatureEntity 重构说明

## 概述

`GeoFeatureEntity` 已从数据库实体类重构为中间态数据传输对象，不再直接映射到 `geo_features` 数据库表。

## 主要变更

### 1. 实体类变更

#### 移除的注解
- `@TableName("geo_features")` - 移除表映射
- `@TableId(value = "id", type = IdType.AUTO)` - 移除主键映射
- `@TableField("feature_id")` - 移除字段映射
- `@TableField("geometry")` - 移除字段映射
- `@TableField("attributes")` - 移除字段映射
- `@TableField(value = "created_at", fill = FieldFill.INSERT)` - 移除自动填充

#### 新增功能
- 添加 `rawAttributes` 字段用于存储原始属性映射
- 添加便捷方法：`getAttribute()`, `setAttribute()`, `hasAttribute()`
- 增强 `toString()` 方法，提供更好的调试信息
- 添加详细的类注释说明其用途

### 2. 相关服务变更

#### BatchInsertService
- **移除**: 继承 `ServiceImpl<GeoFeatureMapper, GeoFeatureEntity>`
- **移除**: `batchInsertGeoFeatures()` 和 `fastBatchInsert()` 方法
- **新增**: `preprocessGeoFeatures()` - 数据预处理
- **新增**: `batchPreprocessGeoFeatures()` - 批量预处理
- **新增**: `validateGeoFeatures()` - 数据验证
- **新增**: `getStatistics()` - 统计信息

#### GeoFeatureMapper
- **完全移除**: 不再需要数据库映射接口

### 3. 使用方式变更

#### 之前的使用方式
```java
// 直接插入到 geo_features 表
batchInsertService.fastBatchInsert(entities);
```

#### 现在的使用方式
```java
// 预处理中间态数据
batchInsertService.batchPreprocessGeoFeatures(entities);

// 通过模板配置插入到目标表
templateBasedDatabaseInsertService.batchInsertWithTemplate(entities, template);
```

## 影响的组件

### 直接影响
1. **GeoFeatureEntity** - 重构为中间态对象
2. **BatchInsertService** - 移除数据库操作，改为数据处理服务
3. **GeoFeatureMapper** - 完全移除
4. **MyBatisPlusConfig** - 更新自动填充配置

### 间接影响
1. **TemplateBasedDatabaseInsertServiceImpl** - 更新服务调用
2. **ShapefileReaderImpl** - 更新批量处理逻辑
3. **所有使用 GeoFeatureEntity 的控制器和服务** - 需要适配新的使用方式

## 迁移指南

### 对于开发者

1. **不要直接操作 geo_features 表**
   - 使用模板配置进行数据插入
   - 通过 `TemplateBasedDatabaseInsertService` 进行数据库操作

2. **使用新的数据处理方法**
   ```java
   // 数据预处理
   batchInsertService.batchPreprocessGeoFeatures(entities);
   
   // 数据验证
   boolean isValid = batchInsertService.validateGeoFeatures(entities);
   
   // 获取统计信息
   String stats = batchInsertService.getStatistics(entities);
   ```

3. **利用新的属性访问方法**
   ```java
   // 设置原始属性
   entity.setAttribute("fieldName", value);
   
   // 获取属性值
   Object value = entity.getAttribute("fieldName");
   
   // 检查属性存在
   boolean exists = entity.hasAttribute("fieldName");
   ```

### 对于系统集成

1. **确保模板配置正确**
   - 验证 `GisManageTemplate` 配置
   - 确保目标表结构与模板匹配

2. **更新数据流程**
   - Shapefile 读取 → GeoFeatureEntity（中间态）→ 模板转换 → 目标表插入

3. **监控和日志**
   - 关注新的警告日志
   - 验证数据是否正确插入到目标表

## 优势

1. **更灵活的数据处理** - 不再绑定到固定的 geo_features 表结构
2. **更好的模板支持** - 完全基于模板配置进行数据转换
3. **减少数据库依赖** - 中间态对象不依赖特定的数据库表
4. **更清晰的职责分离** - 数据传输对象与数据库实体分离

## 注意事项

1. **向后兼容性** - 此变更可能影响现有的数据导入流程
2. **性能考虑** - 确保新的数据处理流程性能满足要求
3. **错误处理** - 注意处理模板配置错误和数据验证失败的情况
4. **测试覆盖** - 需要更新相关的单元测试和集成测试

## 后续计划

1. **完全移除 geo_features 表依赖** - 在确认所有功能正常后
2. **优化模板化数据处理性能** - 针对大批量数据处理进行优化
3. **增强数据验证功能** - 基于模板配置进行更严格的数据验证
