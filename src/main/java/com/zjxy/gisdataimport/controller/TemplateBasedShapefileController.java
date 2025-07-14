package com.zjxy.gisdataimport.controller;

import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.service.GisManageTemplateService;
import com.zjxy.gisdataimport.service.TemplateBasedShapefileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于模板的Shapefile处理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/template-shapefile")
public class TemplateBasedShapefileController {

    @Autowired
    private TemplateBasedShapefileService templateShapefileService;

    @Autowired
    private GisManageTemplateService templateService;

    /**
     * 使用模板上传并处理Shapefile
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
            log.info("开始使用模板处理Shapefile，模板ID: {}, 文件名: {}", templateId, file.getOriginalFilename());
            
            // 使用模板处理Shapefile
            Map<String, Object> result = templateShapefileService.processShapefileWithTemplate(
                file.getInputStream(),
                file.getOriginalFilename(),
                templateId
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("使用模板处理Shapefile出错", e);
            response.put("success", false);
            response.put("message", "处理Shapefile出错: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 从路径使用模板处理Shapefile
     */
    @PostMapping("/process-with-template")
    public ResponseEntity<Map<String, Object>> processShapefileWithTemplate(
            @RequestParam("filePath") String filePath,
            @RequestParam("templateId") Integer templateId) {
        
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("从路径使用模板处理Shapefile，模板ID: {}, 文件路径: {}", templateId, filePath);
            
            // 使用模板处理Shapefile
            Map<String, Object> result = templateShapefileService.processShapefileWithTemplateFromPath(filePath, templateId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("使用模板处理Shapefile出错", e);
            response.put("success", false);
            response.put("message", "处理Shapefile出错: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取所有导入模板
     */
    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getAllImportTemplates() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<GisManageTemplate> templates = templateService.getAllImportTemplates();
            
            response.put("success", true);
            response.put("data", templates);
            response.put("total", templates.size());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            response.put("success", false);
            response.put("message", "获取模板列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 根据ID获取模板详情
     */
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<Map<String, Object>> getTemplateById(@PathVariable Integer templateId) {
        Map<String, Object> response = new HashMap<>();

        try {
            GisManageTemplate template = templateService.getTemplateById(templateId);
            
            response.put("success", true);
            response.put("data", template);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取模板详情失败，模板ID: {}", templateId, e);
            response.put("success", false);
            response.put("message", "获取模板详情失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 创建新模板
     */
    @PostMapping("/templates")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody GisManageTemplate template) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer templateId = templateService.createTemplate(template);
            
            response.put("success", true);
            response.put("message", "模板创建成功");
            response.put("templateId", templateId);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("创建模板失败", e);
            response.put("success", false);
            response.put("message", "创建模板失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新模板
     */
    @PutMapping("/templates/{templateId}")
    public ResponseEntity<Map<String, Object>> updateTemplate(
            @PathVariable Integer templateId,
            @RequestBody GisManageTemplate template) {
        
        Map<String, Object> response = new HashMap<>();

        try {
            template.setId(templateId);
            Boolean success = templateService.updateTemplate(template);
            
            response.put("success", success);
            response.put("message", success ? "模板更新成功" : "模板更新失败");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("更新模板失败，模板ID: {}", templateId, e);
            response.put("success", false);
            response.put("message", "更新模板失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable Integer templateId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Boolean success = templateService.deleteTemplate(templateId);
            
            response.put("success", success);
            response.put("message", success ? "模板删除成功" : "模板删除失败");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("删除模板失败，模板ID: {}", templateId, e);
            response.put("success", false);
            response.put("message", "删除模板失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根据几何类型获取模板列表
     */
    @GetMapping("/templates/by-geometry-type/{geometryType}")
    public ResponseEntity<Map<String, Object>> getTemplatesByGeometryType(@PathVariable Integer geometryType) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<GisManageTemplate> templates = templateService.getTemplatesByGeometryType(geometryType);
            
            response.put("success", true);
            response.put("data", templates);
            response.put("total", templates.size());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("根据几何类型获取模板列表失败，几何类型: {}", geometryType, e);
            response.put("success", false);
            response.put("message", "获取模板列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 根据表名获取模板列表
     */
    @GetMapping("/templates/by-table-name/{tableName}")
    public ResponseEntity<Map<String, Object>> getTemplatesByTableName(@PathVariable String tableName) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<GisManageTemplate> templates = templateService.getTemplatesByTableName(tableName);
            
            response.put("success", true);
            response.put("data", templates);
            response.put("total", templates.size());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("根据表名获取模板列表失败，表名: {}", tableName, e);
            response.put("success", false);
            response.put("message", "获取模板列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 验证模板配置
     */
    @PostMapping("/templates/validate")
    public ResponseEntity<Map<String, Object>> validateTemplate(@RequestBody GisManageTemplate template) {
        Map<String, Object> response = new HashMap<>();

        try {
            Boolean isValid = templateService.validateTemplate(template);
            
            response.put("success", true);
            response.put("valid", isValid);
            response.put("message", isValid ? "模板配置有效" : "模板配置无效");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("验证模板配置失败", e);
            response.put("success", false);
            response.put("message", "验证模板配置失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
