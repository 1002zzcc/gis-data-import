package com.zjxy.gisdataimport.controller;

import com.zjxy.gisdataimport.shap.ShapefileReader;
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
}
