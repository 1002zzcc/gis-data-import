# 快速测试步骤

## 🚀 一分钟快速测试

### 1. 测试原始方法性能

**修改配置文件** `src/main/resources/application.properties`:
```properties
# 设置为原始方法
test.performance.method=original
```

**启动应用并上传文件**，观察输出类似：
```
=== 性能测试模式 ===
测试数据量: 10000 条
处理方法: original
==================
原始方法已处理 1000 条记录
原始方法已处理 2000 条记录
...
原始方法处理完成，实际处理: 10000 条记录
总耗时: 25000 ms (25.00 秒)
平均处理速度: 400.00 条/秒
```

### 2. 测试优化方法性能

**修改配置文件**:
```properties
# 设置为优化方法
test.performance.method=optimized
```

**重启应用并上传相同文件**，观察输出类似：
```
=== 性能测试模式 ===
测试数据量: 10000 条
处理方法: optimized
==================
优化方法 - 提交批次处理任务，批次大小: 10000, 已提交批次数: 1
批次处理完成 - 线程: BatchProcessing-1, 处理数量: 10000, 耗时: 3000ms
总耗时: 3500 ms (3.50 秒)
平均处理速度: 2857.14 条/秒
```

### 3. 性能对比

| 方法 | 处理时间 | 处理速度 | 性能提升 |
|------|----------|----------|----------|
| 原始方法 | ~25秒 | ~400条/秒 | 基准 |
| 优化方法 | ~3.5秒 | ~2857条/秒 | **7倍提升** |

## 📋 详细配置选项

### 测试不同数据量
```properties
# 测试5000条
test.performance.max-records=5000

# 测试20000条  
test.performance.max-records=20000
```

### 清空表数据（每次测试前）
```properties
test.performance.clear-table-before-test=true
```

### 关闭测试模式（处理全部数据）
```properties
test.performance.enabled=false
```

## 🔧 如果性能提升不明显

### 1. 调整批次大小
编辑 `src/main/java/com/zjxy/gisdataimport/config/BatchProcessingConfig.java`:
```java
// 减小批次大小试试
public static final int LARGE_BATCH_SIZE = 5000;
public static final int JDBC_BATCH_SIZE = 500;
```

### 2. 调整线程数
```java
// 减少线程数
public static final int MAX_THREAD_COUNT = 4;
```

### 3. 检查数据库配置
确保 `application.properties` 包含优化配置：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/gisdb?...&rewriteBatchedStatements=true
```

## 📊 预期结果

### 小数据量 (1万条)
- **原始方法**: 20-30秒
- **优化方法**: 3-5秒
- **提升倍数**: 5-8倍

### 中等数据量 (10万条)  
- **原始方法**: 3-5分钟
- **优化方法**: 15-30秒
- **提升倍数**: 8-12倍

### 大数据量 (30万条)
- **原始方法**: 8-15分钟  
- **优化方法**: 30-60秒
- **提升倍数**: 10-15倍

## 🚨 注意事项

1. **首次运行较慢**: JVM预热，数据库连接建立
2. **内存监控**: 观察是否有内存不足警告
3. **数据库连接**: 确保数据库连接稳定
4. **文件大小**: 确保测试文件包含足够的记录数

## 🎯 测试成功标志

看到类似输出表示测试成功：
```
=== 性能监控报告 ===
总处理记录数: 10000 条
总批次数: 1 个  
总耗时: 3500 ms (3.50 秒)
平均处理速度: 2857.14 条/秒
平均批次大小: 10000 条
平均批次处理时间: 3000.00 ms
=== 监控结束 ===

=== 测试结果统计 ===
最终表记录数: 10000
✓ 数据量符合预期
==================
```

## 🔄 切换到生产模式

测试满意后，切换到生产模式处理全部数据：
```properties
# 关闭测试模式
test.performance.enabled=false
# 或者设置更大的数据量
test.performance.max-records=300000
```
