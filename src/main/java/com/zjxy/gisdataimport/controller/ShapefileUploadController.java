package com.zjxy.gisdataimport.controller;

import com.zjxy.gisdataimport.shap.ShapefileReader;
import com.zjxy.gisdataimport.service.TemplateBasedShapefileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/shapefiles")
public class ShapefileUploadController {

    @Autowired
    private ShapefileReader shapefileReader;

    @Autowired
    private TemplateBasedShapefileService templateShapefileService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadShapefile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "请选择一个ZIP文件上传");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 处理上传的ZIP文件
            int featuresProcessed = shapefileReader.processShapefileZip(
                file.getInputStream(),
                file.getOriginalFilename()
            );

            response.put("success", true);
            response.put("message", "Shapefile处理成功");
            response.put("featuresProcessed", featuresProcessed);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "处理Shapefile出错: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processShapefileFromPath(@RequestParam("filePath") String filePath) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 从指定路径处理ZIP文件
            int featuresProcessed = shapefileReader.processShapefileZipFromPath(filePath);

            response.put("success", true);
            response.put("message", "Shapefile处理成功");
            response.put("featuresProcessed", featuresProcessed);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "处理Shapefile出错: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 使用模板上传并处理Shapefile（兼容接口）
     */
    @PostMapping("/upload-with-template")
    public ResponseEntity<Map<String, Object>> uploadShapefileWithTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateId") Integer templateId) {

        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "请选择一个ZIP文件上传");
            return ResponseEntity.badRequest().body(response);
        }

        if (templateId == null) {
            response.put("success", false);
            response.put("message", "请指定模板ID");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 使用模板处理Shapefile
            Map<String, Object> result = templateShapefileService.processShapefileWithTemplate(
                file.getInputStream(),
                file.getOriginalFilename(),
                templateId
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "使用模板处理Shapefile出错: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 从路径使用模板处理Shapefile（兼容接口）
     */
    @PostMapping("/process-with-template")
    public ResponseEntity<Map<String, Object>> processShapefileWithTemplate(
            @RequestParam("filePath") String filePath,
            @RequestParam("templateId") Integer templateId) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 使用模板处理Shapefile
            Map<String, Object> result = templateShapefileService.processShapefileWithTemplateFromPath(filePath, templateId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "使用模板处理Shapefile出错: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
