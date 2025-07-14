package com.zjxy.gisdataimport.service;

import com.zjxy.gisdataimport.config.CoordinateTransformConfig;
import com.zjxy.gisdataimport.util.ZbzhUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 坐标转换服务
 * 用于在GIS数据导入过程中进行坐标系转换
 */
@Slf4j
@Service
public class CoordinateTransformService {

    @Autowired
    private CoordinateTransformConfig config;

    /**
     * 转换几何数据的坐标系
     * @param geometryWkt 原始几何数据的WKT字符串
     * @return 转换后的WKT字符串
     */
    public String transformGeometry(String geometryWkt) {
        return transformGeometry(geometryWkt, config.getSourceCoordSystem(), config.getTargetCoordSystem());
    }

    /**
     * 转换几何数据的坐标系
     * @param geometryWkt 原始几何数据的WKT字符串
     * @param sourceCoordSystem 源坐标系
     * @param targetCoordSystem 目标坐标系
     * @return 转换后的WKT字符串
     */
    public String transformGeometry(String geometryWkt, String sourceCoordSystem, String targetCoordSystem) {
        if (geometryWkt == null || geometryWkt.trim().isEmpty()) {
            return geometryWkt;
        }

        // 检查是否启用坐标转换
        if (!config.isEnabled()) {
            return geometryWkt;
        }

        try {
            // 如果源坐标系和目标坐标系相同，直接返回
            if (sourceCoordSystem.equals(targetCoordSystem)) {
                return geometryWkt;
            }

            if (config.isLogTransformation()) {
                log.info("开始坐标转换: {} -> {}, WKT: {}", sourceCoordSystem, targetCoordSystem, geometryWkt);
            }

            String transformedWkt = ZbzhUtil.convertSingleGeometry(geometryWkt, sourceCoordSystem, targetCoordSystem);

            if (config.isLogTransformation()) {
                log.info("坐标转换完成: {}", transformedWkt);
            }

            return transformedWkt;

        } catch (Exception e) {
            log.error("坐标转换失败. WKT: {}, 源坐标系: {}, 目标坐标系: {}",
                     geometryWkt, sourceCoordSystem, targetCoordSystem, e);

            // 根据配置的失败策略处理
            switch (config.getFailureStrategy()) {
                case SET_ERROR:
                    return "COORDINATE_TRANSFORM_ERROR";
                case SKIP_RECORD:
                    return null; // 返回null表示跳过该记录
                case KEEP_ORIGINAL:
                default:
                    return geometryWkt; // 返回原始数据
            }
        }
    }

    /**
     * 使用指定坐标系转换几何数据（模板化转换的别名方法）
     * @param geometryWkt 原始几何数据的WKT字符串
     * @param sourceCoordSystem 源坐标系
     * @param targetCoordSystem 目标坐标系
     * @return 转换后的WKT字符串
     */
    public String transformGeometryWithCoordSystems(String geometryWkt, String sourceCoordSystem, String targetCoordSystem) {
        return transformGeometry(geometryWkt, sourceCoordSystem, targetCoordSystem);
    }

    /**
     * 批量转换几何数据
     * @param geometryWkts 几何数据WKT字符串列表
     * @param sourceCoordSystem 源坐标系
     * @param targetCoordSystem 目标坐标系
     * @return 转换后的WKT字符串列表
     */
    public java.util.List<String> transformGeometries(java.util.List<String> geometryWkts,
                                                      String sourceCoordSystem,
                                                      String targetCoordSystem) {
        java.util.List<String> transformedWkts = new java.util.ArrayList<>();

        for (String wkt : geometryWkts) {
            transformedWkts.add(transformGeometry(wkt, sourceCoordSystem, targetCoordSystem));
        }

        return transformedWkts;
    }

    /**
     * 检查坐标系是否支持
     * @param coordSystem 坐标系名称
     * @return 是否支持
     */
    public boolean isSupportedCoordSystem(String coordSystem) {
        try {
            java.util.Map<String, Object> coordSystems = ZbzhUtil.GetCoordSystem();
            return coordSystems.containsKey(coordSystem);
        } catch (Exception e) {
            log.error("检查坐标系支持状态失败: {}", coordSystem, e);
            return false;
        }
    }

    /**
     * 获取所有支持的坐标系
     * @return 支持的坐标系列表
     */
    public java.util.Set<String> getSupportedCoordSystems() {
        try {
            java.util.Map<String, Object> coordSystems = ZbzhUtil.GetCoordSystem();
            return coordSystems.keySet();
        } catch (Exception e) {
            log.error("获取支持的坐标系列表失败", e);
            return new java.util.HashSet<>();
        }
    }

    /**
     * 设置默认的坐标转换配置
     * 根据您的具体需求调整这些配置
     */
//    public static class CoordTransformConfig {
//        // 常用的坐标系配置
//        public static final String CGCS2000 = "CGCS2000";           // CGCS2000经纬度
//        public static final String CGCS2000XY = "CGCS2000XY";       // CGCS2000投影坐标
//        public static final String WENZHOU2000 = "WenZhou2000";     // 温州2000
//        public static final String WENZHOU_CITY = "WenZhouCity";    // 温州城市坐标系
//        public static final String BEIJING1954 = "Beijing1954";     // 北京1954
//    }
}
