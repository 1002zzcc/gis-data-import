# 性能测试指南

## 测试目的
在处理30万条数据之前，先用1万条数据测试两种处理方法的性能差异。

## 测试配置

### 1. 配置文件设置 (application.properties)

```properties
# ========== 性能测试配置 ==========
# 测试模式开关
test.performance.enabled=true
# 测试数据量限制 (1万条)
test.performance.max-records=10000
# 处理方法选择: original(原始) 或 optimized(优化)
test.performance.method=optimized
# 详细日志开关
test.performance.verbose-logging=true
# 测试前是否清空表
test.performance.clear-table-before-test=false
```

### 2. 测试方法切换

#### 测试原始方法
```properties
test.performance.method=original
```

#### 测试优化方法
```properties
test.performance.method=optimized
```

## 测试步骤

### 第一步：测试原始方法
1. 修改 `application.properties`:
   ```properties
   test.performance.method=original
   ```

2. 启动应用程序

3. 上传您的Shapefile ZIP文件

4. 观察控制台输出，记录：
   - 处理时间
   - 处理速度 (条/秒)
   - 内存使用情况

### 第二步：测试优化方法
1. 修改 `application.properties`:
   ```properties
   test.performance.method=optimized
   ```

2. 重启应用程序

3. 上传相同的Shapefile ZIP文件

4. 观察控制台输出，记录：
   - 处理时间
   - 处理速度 (条/秒)
   - 内存使用情况

### 第三步：对比分析
比较两种方法的性能指标：
- 处理时间差异
- 处理速度提升倍数
- 资源使用情况

## 预期输出示例

### 原始方法输出
```
=== 性能测试模式 ===
测试数据量: 10000 条
处理方法: original
==================
=== 开始性能监控 ===
原始方法已处理 1000 条记录
原始方法已处理 2000 条记录
...
原始方法处理完成，实际处理: 10000 条记录
=== 性能监控报告 ===
总处理记录数: 10000 条
总耗时: 25000 ms (25.00 秒)
平均处理速度: 400.00 条/秒
```

### 优化方法输出
```
=== 性能测试模式 ===
测试数据量: 10000 条
处理方法: optimized
==================
=== 开始性能监控 ===
优化方法 - 提交批次处理任务，批次大小: 10000, 已提交批次数: 1, 累计处理: 10000 条
批次处理完成 - 线程: BatchProcessing-1, 处理数量: 10000, 耗时: 3000ms
=== 性能监控报告 ===
总处理记录数: 10000 条
总耗时: 3500 ms (3.50 秒)
平均处理速度: 2857.14 条/秒
```

## 测试注意事项

### 1. 数据库状态
- 每次测试前建议清空 `geo_features` 表
- 或者设置 `test.performance.clear-table-before-test=true`

### 2. 系统资源监控
- 观察CPU使用率
- 监控内存使用情况
- 注意数据库连接数

### 3. 测试数据
- 使用相同的Shapefile文件进行测试
- 确保数据量足够（至少包含1万条记录）

### 4. 多次测试
- 建议每种方法测试3次取平均值
- 排除首次运行的JVM预热影响

## 调整测试参数

### 修改测试数据量
```properties
# 测试5000条数据
test.performance.max-records=5000

# 测试20000条数据
test.performance.max-records=20000
```

### 关闭测试模式（处理全部数据）
```properties
test.performance.enabled=false
```

## 性能优化参数调整

如果优化方法效果不明显，可以尝试调整以下参数：

### 1. 批次大小调整
在 `BatchProcessingConfig.java` 中：
```java
public static final int LARGE_BATCH_SIZE = 5000; // 减小批次
public static final int JDBC_BATCH_SIZE = 500;   // 减小JDBC批次
```

### 2. 线程数调整
```java
public static final int MAX_THREAD_COUNT = 4; // 减少线程数
```

### 3. 数据库连接池调整
在 `application.properties` 中：
```properties
spring.datasource.hikari.maximum-pool-size=10
```

## 故障排除

### 1. 内存不足
- 减小 `LARGE_BATCH_SIZE`
- 增加JVM内存：`-Xmx2g`

### 2. 数据库连接超时
- 增加连接超时时间
- 减少并发线程数

### 3. 处理速度仍然很慢
- 检查数据库索引
- 确认网络连接稳定
- 验证硬盘I/O性能
