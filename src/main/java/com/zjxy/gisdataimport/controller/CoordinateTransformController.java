package com.zjxy.gisdataimport.controller;

import com.zjxy.gisdataimport.service.CoordinateTransformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 坐标转换测试控制器
 * 用于测试和验证坐标转换功能
 */
@RestController
@RequestMapping("/api/coordinate")
public class CoordinateTransformController {

    @Autowired
    private CoordinateTransformService coordinateTransformService;

    /**
     * 测试单个几何对象的坐标转换
     */
    @PostMapping("/transform")
    public ResponseEntity<Map<String, Object>> transformGeometry(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String wkt = request.get("wkt");
            String sourceCoordSystem = request.get("sourceCoordSystem");
            String targetCoordSystem = request.get("targetCoordSystem");

            if (wkt == null || wkt.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "WKT字符串不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            String transformedWkt;
            if (sourceCoordSystem != null && targetCoordSystem != null) {
                transformedWkt = coordinateTransformService.transformGeometry(wkt, sourceCoordSystem, targetCoordSystem);
            } else {
                transformedWkt = coordinateTransformService.transformGeometry(wkt);
            }

            response.put("success", true);
            response.put("originalWkt", wkt);
            response.put("transformedWkt", transformedWkt);
            response.put("sourceCoordSystem", sourceCoordSystem);
            response.put("targetCoordSystem", targetCoordSystem);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "坐标转换失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取支持的坐标系列表
     */
    @GetMapping("/coord-systems")
    public ResponseEntity<Map<String, Object>> getSupportedCoordSystems() {
        Map<String, Object> response = new HashMap<>();

        try {
            Set<String> coordSystems = coordinateTransformService.getSupportedCoordSystems();
            response.put("success", true);
            response.put("coordSystems", coordSystems);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取坐标系列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 检查坐标系是否支持
     */
    @GetMapping("/coord-systems/{coordSystem}/check")
    public ResponseEntity<Map<String, Object>> checkCoordSystemSupport(@PathVariable String coordSystem) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean supported = coordinateTransformService.isSupportedCoordSystem(coordSystem);
            response.put("success", true);
            response.put("coordSystem", coordSystem);
            response.put("supported", supported);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "检查坐标系支持状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 测试常见几何类型的坐标转换
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testCoordinateTransform() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 测试点
            String pointWkt = "POINT (499045.9906393343 3096924.4116956145)";
            String transformedPoint = coordinateTransformService.transformGeometry(pointWkt);

            // 测试多线
            String multiLineWkt = "MULTILINESTRING ((509052.7857999997 3074945.454, 509067.92819999997 3074950.0625))";
            String transformedMultiLine = coordinateTransformService.transformGeometry(multiLineWkt);

            response.put("success", true);

            // 创建测试结果Map (Java 8兼容方式)
            Map<String, Object> tests = new HashMap<>();

            Map<String, String> pointTest = new HashMap<>();
            pointTest.put("original", pointWkt);
            pointTest.put("transformed", transformedPoint);
            tests.put("point", pointTest);

            Map<String, String> multiLineTest = new HashMap<>();
            multiLineTest.put("original", multiLineWkt);
            multiLineTest.put("transformed", transformedMultiLine);
            tests.put("multiLineString", multiLineTest);

            response.put("tests", tests);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "测试坐标转换失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
