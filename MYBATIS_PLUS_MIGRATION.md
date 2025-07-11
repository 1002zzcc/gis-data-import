# MyBatis-Plus + Druid 迁移指南

## 🔄 迁移概述

已成功将项目从 **JPA + HikariCP** 迁移到 **MyBatis-Plus + Druid连接池**，以获得更好的性能和更灵活的SQL控制。

## 📦 依赖变更

### 移除的依赖
```xml
<!-- 移除JPA依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

### 新增的依赖
```xml
<!-- MyBatis-Plus依赖 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.3.1</version>
</dependency>

<!-- 阿里Druid连接池 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.16</version>
</dependency>
```

## ⚙️ 配置变更

### 数据库连接配置
```properties
# 使用Druid连接池
spring.datasource.type=com.alibaba.druid.pool.DruidDataSource
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Druid连接池配置
spring.datasource.druid.initial-size=5
spring.datasource.druid.min-idle=5
spring.datasource.druid.max-active=20
spring.datasource.druid.max-wait=60000
spring.datasource.druid.validation-query=SELECT 1 FROM DUAL
spring.datasource.druid.test-while-idle=true
```

### MyBatis-Plus配置
```properties
# MyBatis-Plus配置
mybatis-plus.type-aliases-package=com.zjxy.gisdataimport.shap
mybatis-plus.configuration.map-underscore-to-camel-case=true
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
```

### Druid监控配置
```properties
# 访问地址: http://localhost:8080/druid/
spring.datasource.druid.stat-view-servlet.enabled=true
spring.datasource.druid.stat-view-servlet.url-pattern=/druid/*
spring.datasource.druid.stat-view-servlet.login-username=admin
spring.datasource.druid.stat-view-servlet.login-password=admin123
```

## 🏗️ 代码变更

### 1. 实体类变更
```java
// 原JPA注解
@Entity
@Table(name = "geo_features")
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "feature_id")

// 改为MyBatis-Plus注解
@TableName("geo_features")
@TableId(value = "id", type = IdType.AUTO)
@TableField("feature_id")
@TableField(value = "created_at", fill = FieldFill.INSERT)
```

### 2. Repository变更
```java
// 原JPA Repository
public interface GeoFeatureRepository extends JpaRepository<GeoFeatureEntity, Long>

// 改为MyBatis-Plus Mapper
@Mapper
public interface GeoFeatureRepository extends BaseMapper<GeoFeatureEntity>
```

### 3. 方法调用变更
```java
// JPA方法 → MyBatis-Plus方法
repository.save(entity) → repository.insert(entity)
repository.saveAll(list) → batchInsertService.fastBatchInsert(list)
repository.count() → repository.selectCount(null)
repository.deleteAll() → repository.delete(null)
```

## 🚀 性能优化特性

### 1. Druid连接池优势
- **监控功能**: 实时监控SQL执行情况
- **防SQL注入**: 内置Wall防火墙
- **连接泄露检测**: 自动检测连接泄露
- **性能统计**: 详细的性能统计信息

### 2. MyBatis-Plus优势
- **批量操作**: 高效的批量插入/更新
- **代码生成**: 自动生成基础CRUD代码
- **分页插件**: 物理分页，性能更好
- **条件构造器**: 类型安全的SQL构造

### 3. 批量插入优化
```java
// 新的批量插入服务
@Service
public class BatchInsertService extends ServiceImpl<GeoFeatureRepository, GeoFeatureEntity> {
    public void fastBatchInsert(List<GeoFeatureEntity> entities) {
        // 分批插入，每批1000条
        this.saveBatch(entities, 1000);
    }
}
```

## 📊 监控功能

### Druid监控面板
访问: `http://localhost:8080/druid/`
- 用户名: `admin`
- 密码: `admin123`

### 监控功能包括:
- **数据源监控**: 连接池状态、活跃连接数
- **SQL监控**: 执行时间、执行次数、慢SQL
- **URI监控**: 接口调用统计
- **Session监控**: 会话信息

## 🗄️ 数据库表结构

```sql
CREATE TABLE geo_features (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    feature_id VARCHAR(255) NOT NULL COMMENT '要素ID',
    geometry LONGTEXT COMMENT '几何信息(WKT格式)',
    attributes LONGTEXT COMMENT '属性信息(JSON格式)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_feature_id (feature_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 🔧 配置文件

### 关键配置项
```properties
# 测试配置保持不变
test.performance.enabled=true
test.performance.max-records=10000
test.performance.method=optimized

# 新增Druid配置
spring.datasource.druid.filters=stat,wall,slf4j
spring.datasource.druid.connection-properties=druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000
```

## ✅ 迁移验证

### 1. 启动验证
- 应用正常启动
- 数据库连接成功
- Druid监控页面可访问

### 2. 功能验证
- 文件上传功能正常
- 数据插入功能正常
- 性能测试功能正常

### 3. 性能验证
- 批量插入性能提升
- 连接池监控正常
- SQL执行监控正常

## 🎯 预期性能提升

### MyBatis-Plus批量插入
- **JPA saveAll**: ~1000条/秒
- **MyBatis-Plus saveBatch**: ~3000-5000条/秒
- **性能提升**: 3-5倍

### Druid连接池
- **更好的连接管理**: 减少连接创建开销
- **监控功能**: 便于性能调优
- **防护功能**: 提高系统安全性

## 🚨 注意事项

1. **自动填充**: 创建时间通过MyBatis-Plus自动填充
2. **批量操作**: 使用专门的BatchInsertService
3. **监控密码**: 生产环境请修改Druid监控密码
4. **SQL日志**: 生产环境建议关闭SQL日志输出
