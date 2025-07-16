# 更新后的支持坐标系转换

## 当前支持的坐标系

系统目前支持以下 **5个坐标系**：

| 坐标系名称 | 类型 | 椭球体 | 中央经线 | 用途 | 适用范围 |
|-----------|------|--------|----------|------|----------|
| **CGCS2000** | 经纬度坐标 | CGCS2000 | 120.000000° | 中国大地坐标系2000，经纬度坐标 | 全国通用 |
| **CGCS2000XY** | 投影坐标 | CGCS2000 | 120.000000° | 中国大地坐标系2000，投影坐标 | 全国通用 |
| **WenZhou2000** | 投影坐标 | CGCS2000 | 120.666667° | 温州2000坐标系，投影坐标 | 温州地区 |
| **WenZhouCity** | 投影坐标 | Beijing1954 | 120.666667° | 温州城市坐标系，投影坐标 | 温州城区 |
| **Beijing1954** | 投影坐标 | Beijing1954 | 120.000000° | 北京1954坐标系，投影坐标 | 历史坐标系 |

## 支持的坐标转换对

系统支持以下 **8个转换对**（移除了失败的 WenZhou2000 ↔ CGCS2000XY）：

### 直接转换（4个）
| 源坐标系 | 目标坐标系 | 转换参数状态 | 说明 |
|---------|-----------|-------------|------|
| **WenZhou2000** → **WenZhouCity** | ✅ 正常 | 温州2000坐标系转温州城市坐标系 |
| **Beijing1954** → **WenZhouCity** | ✅ 正常 | 北京1954坐标系转温州城市坐标系 |
| **WenZhou2000** → **CGCS2000** | ✅ 正常 | 温州2000坐标系转国家坐标系（经纬度） |
| **CGCS2000XY** → **CGCS2000** | ✅ 正常 | 国家投影坐标转经纬度坐标 |

### 反向转换（4个）
| 源坐标系 | 目标坐标系 | 转换参数状态 | 说明 |
|---------|-----------|-------------|------|
| **WenZhouCity** → **WenZhou2000** | ✅ 正常 | 温州城市坐标系转温州2000坐标系 |
| **WenZhouCity** → **Beijing1954** | ✅ 正常 | 温州城市坐标系转北京1954坐标系 |
| **CGCS2000** → **WenZhou2000** | ✅ 正常 | 国家坐标系（经纬度）转温州2000坐标系 |
| **CGCS2000** → **CGCS2000XY** | ✅ 正常 | 经纬度坐标转国家投影坐标 |

### 移除的转换对
| 转换对 | 状态 | 原因 |
|--------|------|------|
| ~~WenZhou2000 → CGCS2000XY~~ | ❌ 已移除 | 转换失败，参数配置有问题 |
| ~~CGCS2000XY → WenZhou2000~~ | ❌ 已移除 | 反向转换也失败 |

## 常用转换场景

### 1. 经纬度数据处理
```java
// 经纬度坐标 (如: 120.672, 28.0)
String wkt = "POINT(120.672 28.0)";

// 转换为温州投影坐标
String wenZhouXY = ZbzhUtil.convertSingleGeometry(wkt, "CGCS2000", "WenZhou2000");

// 转换为国家投影坐标
String nationalXY = ZbzhUtil.convertSingleGeometry(wkt, "CGCS2000", "CGCS2000XY");
```

### 2. 温州投影坐标处理
```java
// 温州投影坐标 (如: 337641.456, 3379401.623)
String wkt = "POINT(337641.456 3379401.623)";

// 转换为经纬度
String lonLat = ZbzhUtil.convertSingleGeometry(wkt, "WenZhou2000", "CGCS2000");

// 转换为温州城市坐标
String cityXY = ZbzhUtil.convertSingleGeometry(wkt, "WenZhou2000", "WenZhouCity");

// ❌ 不支持：转换为国家投影坐标（已移除）
// String nationalXY = ZbzhUtil.convertSingleGeometry(wkt, "WenZhou2000", "CGCS2000XY");
```

### 3. 国家投影坐标处理
```java
// 国家投影坐标 (如: 566100.0, 3098623.7)
String wkt = "POINT(566100.0 3098623.7)";

// 转换为经纬度
String lonLat = ZbzhUtil.convertSingleGeometry(wkt, "CGCS2000XY", "CGCS2000");

// ❌ 不支持：转换为温州投影坐标（已移除）
// String wenZhouXY = ZbzhUtil.convertSingleGeometry(wkt, "CGCS2000XY", "WenZhou2000");
```

## 坐标系选择指南

### 根据数据特征选择坐标系

1. **经纬度数据** (如: 120.672, 28.0)
   - 数值范围：经度 -180° ~ 180°，纬度 -90° ~ 90°
   - 使用坐标系：`CGCS2000`

2. **温州地区投影坐标** (如: 337641.456, 3379401.623)
   - 数值范围：X: 200,000 ~ 500,000，Y: 3,000,000 ~ 3,500,000
   - 使用坐标系：`WenZhou2000`

3. **国家投影坐标** (如: 566100.0, 3098623.7)
   - 数值范围：X: -2,000,000 ~ 2,000,000，Y: 1,000,000 ~ 6,000,000
   - 使用坐标系：`CGCS2000XY`

4. **温州城市坐标** (如: 338000, 3380000)
   - 数值范围：类似温州投影坐标，但基于Beijing1954椭球体
   - 使用坐标系：`WenZhouCity`

### 自动检测坐标类型
```java
String detectedType = ZbzhUtil.detectCoordinateType("POINT(120.672 28.0)");
// 返回: "CGCS2000"
```

## 转换限制和注意事项

### 1. 不支持的转换
- ❌ **WenZhou2000 ↔ CGCS2000XY**：转换参数有问题，已移除
- ❌ **Beijing1954 ↔ CGCS2000/CGCS2000XY**：除了通过WenZhouCity中转外，不支持直接转换

### 2. 推荐的转换路径
如果需要在不支持的坐标系之间转换，可以通过中间坐标系：

```java
// WenZhou2000 → CGCS2000XY (通过CGCS2000中转)
String step1 = ZbzhUtil.convertSingleGeometry(wkt, "WenZhou2000", "CGCS2000");
String result = ZbzhUtil.convertSingleGeometry(step1, "CGCS2000", "CGCS2000XY");
```

### 3. 错误处理
- 转换结果不合理时，返回原始数据
- 提供详细的警告日志
- 建议检查坐标系选择

## 前端页面更新

前端 `coordinate-transform.html` 页面已更新：

### 移除的内容
- ❌ 移除了 "WenZhou2000 → CGCS2000XY" 转换对显示
- ❌ 移除了 "CGCS2000XY → WenZhou2000" 反向转换对显示

### 当前显示的转换对
- ✅ 4个直接转换对
- ✅ 4个反向转换对
- ✅ 4种常用转换场景

## API 使用示例

### 支持的转换
```javascript
// ✅ 支持的转换
fetch('/api/coordinate/transform', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        wkt: "POINT(337641.456 3379401.623)",
        sourceCoordSystem: "WenZhou2000",
        targetCoordSystem: "CGCS2000"  // ✅ 支持
    })
})
```

### 不支持的转换
```javascript
// ❌ 不支持的转换
fetch('/api/coordinate/transform', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        wkt: "POINT(337641.456 3379401.623)",
        sourceCoordSystem: "WenZhou2000",
        targetCoordSystem: "CGCS2000XY"  // ❌ 不支持，已移除
    })
})
```

## 总结

更新后的坐标转换系统：

✅ **支持8个可靠的转换对**  
✅ **移除了失败的转换对**  
✅ **提供清晰的使用指导**  
✅ **支持通过中间坐标系的间接转换**  
✅ **前端页面与后端完全同步**  

用户现在可以使用经过验证的、可靠的坐标转换功能，避免了之前失败的转换对带来的困扰。
