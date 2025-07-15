package com.zjxy.gisdataimport.controller;

import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.service.GisManageTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 模板调试控制器
 * 用于调试模板配置问题
 */
@Slf4j
@RestController
@RequestMapping("/api/template-debug")
public class TemplateDebugController {

    @Autowired
    private GisManageTemplateService templateService;

    /**
     * 调试模板信息
     */
    @GetMapping("/template/{templateId}")
    public ResponseEntity<Map<String, Object>> debugTemplate(@PathVariable Integer templateId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("开始调试模板ID: {}", templateId);
            
            // 获取模板信息
            GisManageTemplate template = templateService.getTemplateById(templateId);
            
            // 构建详细的调试信息
            Map<String, Object> templateInfo = new HashMap<>();
            templateInfo.put("id", template.getId());
            templateInfo.put("tableName", template.getTableName());
            templateInfo.put("nameZh", template.getNameZh());
            templateInfo.put("nameEn", template.getNameEn());
            templateInfo.put("originalCoordinateSystem", template.getOriginalCoordinateSystem());
            templateInfo.put("targetCoordinateSystem", template.getTargetCoordinateSystem());
            templateInfo.put("isZh", template.getIsZh());
            templateInfo.put("type", template.getType());
            templateInfo.put("templateType", template.getTemplateType());
            templateInfo.put("dataBase", template.getDataBase());
            templateInfo.put("dataBaseMode", template.getDataBaseMode());
            templateInfo.put("dataBaseTable", template.getDataBaseTable());
            templateInfo.put("inOrOut", template.getInOrOut());
            templateInfo.put("mapJson", template.getMapJson());
            templateInfo.put("lineMapJson", template.getLineMapJson());
            templateInfo.put("pointMapJson", template.getPointMapJson());
            templateInfo.put("createTime", template.getCreateTime());
            
            // 检查关键字段是否为空
            Map<String, Object> validation = new HashMap<>();
            validation.put("hasTableName", template.getTableName() != null && !template.getTableName().trim().isEmpty());
            validation.put("hasNameZh", template.getNameZh() != null && !template.getNameZh().trim().isEmpty());
            validation.put("hasMapJson", template.getMapJson() != null && !template.getMapJson().trim().isEmpty());
            validation.put("hasCoordinateSystem", 
                template.getOriginalCoordinateSystem() != null && !template.getOriginalCoordinateSystem().trim().isEmpty());
            
            // 尝试解析JSON字段
            Map<String, Object> jsonParsing = new HashMap<>();
            try {
                if (template.getMapJson() != null && !template.getMapJson().trim().isEmpty()) {
                    java.util.List<java.util.Map<String, Object>> mapConfig = template.getMap();
                    jsonParsing.put("mapJsonValid", true);
                    jsonParsing.put("mapConfigSize", mapConfig != null ? mapConfig.size() : 0);
                    jsonParsing.put("mapConfig", mapConfig);
                } else {
                    jsonParsing.put("mapJsonValid", false);
                    jsonParsing.put("mapJsonContent", template.getMapJson());
                }
            } catch (Exception e) {
                jsonParsing.put("mapJsonValid", false);
                jsonParsing.put("mapJsonError", e.getMessage());
            }
            
            // 判断是否使用自定义表
            boolean isCustomTable = template.getTableName() != null && 
                                   !template.getTableName().trim().isEmpty() && 
                                   !"geo_features".equals(template.getTableName());
            
            response.put("success", true);
            response.put("templateInfo", templateInfo);
            response.put("validation", validation);
            response.put("jsonParsing", jsonParsing);
            response.put("isCustomTable", isCustomTable);
            response.put("targetTable", isCustomTable ? template.getTableName() : "geo_features");
            
            log.info("模板调试完成，模板ID: {}, 目标表: {}, 是否自定义表: {}", 
                    templateId, isCustomTable ? template.getTableName() : "geo_features", isCustomTable);
            
        } catch (Exception e) {
            log.error("调试模板失败，模板ID: {}", templateId, e);
            response.put("success", false);
            response.put("message", "调试失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查模板的字段映射配置
     */
    @GetMapping("/template/{templateId}/field-mapping")
    public ResponseEntity<Map<String, Object>> debugFieldMapping(@PathVariable Integer templateId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            GisManageTemplate template = templateService.getTemplateById(templateId);
            
            // 检查字段映射
            Map<String, Object> fieldMappingInfo = new HashMap<>();
            
            if (template.getMapJson() != null && !template.getMapJson().trim().isEmpty()) {
                try {
                    java.util.List<java.util.Map<String, Object>> mapConfig = template.getMap();
                    fieldMappingInfo.put("hasMapConfig", true);
                    fieldMappingInfo.put("configCount", mapConfig != null ? mapConfig.size() : 0);

                    if (mapConfig != null) {
                        for (int i = 0; i < mapConfig.size(); i++) {
                            Map<String, Object> fieldConfig = mapConfig.get(i);
                            fieldMappingInfo.put("field_" + i, fieldConfig);
                        }
                    }
                } catch (Exception e) {
                    fieldMappingInfo.put("hasMapConfig", false);
                    fieldMappingInfo.put("parseError", e.getMessage());
                }
            } else {
                fieldMappingInfo.put("hasMapConfig", false);
                fieldMappingInfo.put("mapJsonContent", template.getMapJson());
            }
            
            response.put("success", true);
            response.put("templateId", templateId);
            response.put("fieldMapping", fieldMappingInfo);
            
        } catch (Exception e) {
            log.error("调试字段映射失败，模板ID: {}", templateId, e);
            response.put("success", false);
            response.put("message", "调试失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 测试模板的数据库插入配置
     */
    @PostMapping("/template/{templateId}/test-insert")
    public ResponseEntity<Map<String, Object>> testTemplateInsert(@PathVariable Integer templateId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            GisManageTemplate template = templateService.getTemplateById(templateId);
            
            // 模拟插入测试
            Map<String, Object> insertTest = new HashMap<>();
            insertTest.put("templateId", templateId);
            insertTest.put("tableName", template.getTableName());
            insertTest.put("isCustomTable", template.getTableName() != null && 
                                          !template.getTableName().trim().isEmpty() && 
                                          !"geo_features".equals(template.getTableName()));
            
            // 检查表名配置
            if (template.getTableName() == null || template.getTableName().trim().isEmpty()) {
                insertTest.put("tableNameStatus", "EMPTY");
                insertTest.put("willUseDefaultTable", true);
                insertTest.put("insertSupported", false);
                insertTest.put("reason", "表名为空，将尝试使用默认表，但默认表插入已禁用");
            } else if ("geo_features".equals(template.getTableName())) {
                insertTest.put("tableNameStatus", "DEFAULT");
                insertTest.put("willUseDefaultTable", true);
                insertTest.put("insertSupported", false);
                insertTest.put("reason", "配置为默认表 geo_features，但默认表插入已禁用");
            } else {
                insertTest.put("tableNameStatus", "CUSTOM");
                insertTest.put("willUseDefaultTable", false);
                insertTest.put("insertSupported", true);
                insertTest.put("targetTable", template.getTableName());
                insertTest.put("reason", "将使用自定义表: " + template.getTableName());
            }
            
            response.put("success", true);
            response.put("insertTest", insertTest);
            
        } catch (Exception e) {
            log.error("测试模板插入配置失败，模板ID: {}", templateId, e);
            response.put("success", false);
            response.put("message", "测试失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
