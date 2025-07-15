package com.zjxy.gisdataimport.example;

import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.service.CoordinateTransformService;
import com.zjxy.gisdataimport.service.TemplateBasedShapefileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 模板坐标转换示例
 * 演示如何根据模板中的originalCoordinateSystem，targetCoordinateSystem，isZh字段进行坐标转换
 */
@Slf4j
@Component
public class TemplateCoordinateTransformExample {

    @Autowired
    private TemplateBasedShapefileService templateBasedShapefileService;

    @Autowired
    private CoordinateTransformService coordinateTransformService;

    /**
     * 演示模板坐标转换的使用方法
     */
    public void demonstrateTemplateCoordinateTransform() {
        log.info("=== 模板坐标转换示例 ===");

        // 示例几何数据（经纬度坐标）
        String geometryWkt = "POINT(120.5 30.5)";
        log.info("原始几何数据: {}", geometryWkt);

        // 示例1：启用坐标转换
        GisManageTemplate template1 = createTemplate(true, "CGCS2000", "CGCS2000XY");
        String result1 = templateBasedShapefileService.applyCoordinateTransformWithTemplate(geometryWkt, template1);
        log.info("示例1 - 启用坐标转换 ({} -> {}): {}", 
                template1.getOriginalCoordinateSystem(), 
                template1.getTargetCoordinateSystem(), 
                result1);

        // 示例2：禁用坐标转换
        GisManageTemplate template2 = createTemplate(false, "CGCS2000", "CGCS2000XY");
        String result2 = templateBasedShapefileService.applyCoordinateTransformWithTemplate(geometryWkt, template2);
        log.info("示例2 - 禁用坐标转换 (isZh=false): {}", result2);

        // 示例3：缺少坐标系配置
        GisManageTemplate template3 = createTemplate(true, null, "CGCS2000XY");
        String result3 = templateBasedShapefileService.applyCoordinateTransformWithTemplate(geometryWkt, template3);
        log.info("示例3 - 缺少源坐标系配置: {}", result3);

        // 示例4：相同的源和目标坐标系
        GisManageTemplate template4 = createTemplate(true, "CGCS2000", "CGCS2000");
        String result4 = templateBasedShapefileService.applyCoordinateTransformWithTemplate(geometryWkt, template4);
        log.info("示例4 - 相同的源和目标坐标系: {}", result4);

        log.info("=== 示例完成 ===");
    }

    /**
     * 创建测试模板
     */
    private GisManageTemplate createTemplate(Boolean isZh, String originalCoordSystem, String targetCoordSystem) {
        GisManageTemplate template = new GisManageTemplate();
        template.setId(1);
        template.setNameZh("测试模板");
        template.setTableName("test_table");
        template.setIsZh(isZh);
        template.setOriginalCoordinateSystem(originalCoordSystem);
        template.setTargetCoordinateSystem(targetCoordSystem);
        return template;
    }

    /**
     * 检查坐标系支持情况
     */
    public void checkCoordinateSystemSupport() {
        log.info("=== 检查坐标系支持情况 ===");

        String[] coordSystems = {
            "CGCS2000",
            "CGCS2000XY", 
            "WenZhou2000",
            "WenZhouCity",
            "Beijing1954"
        };

        for (String coordSystem : coordSystems) {
            boolean isSupported = coordinateTransformService.isSupportedCoordSystem(coordSystem);
            log.info("坐标系 {} 支持状态: {}", coordSystem, isSupported ? "支持" : "不支持");
        }

        log.info("所有支持的坐标系: {}", coordinateTransformService.getSupportedCoordSystems());
        log.info("=== 检查完成 ===");
    }

    /**
     * 演示不同坐标系之间的转换
     */
    public void demonstrateCoordinateSystemConversions() {
        log.info("=== 坐标系转换示例 ===");

        String geometryWkt = "POINT(120.5 30.5)";
        log.info("原始几何数据: {}", geometryWkt);

        // 常见的坐标系转换组合
        String[][] conversions = {
            {"CGCS2000", "CGCS2000XY"},
            {"CGCS2000XY", "CGCS2000"},
            {"WenZhou2000", "WenZhouCity"},
            {"Beijing1954", "CGCS2000"}
        };

        for (String[] conversion : conversions) {
            String sourceCoord = conversion[0];
            String targetCoord = conversion[1];

            try {
                GisManageTemplate template = createTemplate(true, sourceCoord, targetCoord);
                String result = templateBasedShapefileService.applyCoordinateTransformWithTemplate(geometryWkt, template);
                log.info("转换 {} -> {}: {}", sourceCoord, targetCoord, result);
            } catch (Exception e) {
                log.warn("转换失败 {} -> {}: {}", sourceCoord, targetCoord, e.getMessage());
            }
        }

        log.info("=== 转换示例完成 ===");
    }
}
