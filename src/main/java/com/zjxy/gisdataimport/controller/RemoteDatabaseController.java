package com.zjxy.gisdataimport.controller;

import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.service.GisManageTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 远程数据库连接和模板验证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/remote-db")
public class RemoteDatabaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GisManageTemplateService templateService;

    /**
     * 测试远程数据库连接
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 测试数据库连接
            String result = jdbcTemplate.queryForObject("SELECT version()", String.class);
            
            response.put("success", true);
            response.put("message", "远程数据库连接成功");
            response.put("databaseVersion", result);
            
            // 检查当前数据库名
            String currentDb = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
            response.put("currentDatabase", currentDb);
            
            log.info("远程数据库连接测试成功: {}", currentDb);
            
        } catch (Exception e) {
            log.error("远程数据库连接测试失败", e);
            response.put("success", false);
            response.put("message", "数据库连接失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查模板表是否存在
     */
    @GetMapping("/check-template-table")
    public ResponseEntity<Map<String, Object>> checkTemplateTable() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查 gis_manage_template 表是否存在
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'gis_manage_template'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            
            if (count != null && count > 0) {
                // 表存在，获取模板数量
                Integer templateCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gis_manage_template", Integer.class);
                
                response.put("success", true);
                response.put("tableExists", true);
                response.put("templateCount", templateCount);
                response.put("message", "模板表存在，包含 " + templateCount + " 个模板");
            } else {
                response.put("success", false);
                response.put("tableExists", false);
                response.put("message", "gis_manage_template 表不存在");
            }
            
        } catch (Exception e) {
            log.error("检查模板表失败", e);
            response.put("success", false);
            response.put("message", "检查失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取可用的模板列表
     */
    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getAvailableTemplates() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 直接查询数据库获取模板列表
            String sql = "SELECT " +
                "id, " +
                "table_name, " +
                "name_zh, " +
                "original_coordinate_system, " +
                "target_coordinate_system, " +
                "is_zh, " +
                "template_type, " +
                "in_or_out, " +
                "CASE " +
                    "WHEN table_name IS NULL OR table_name = '' THEN 'EMPTY' " +
                    "WHEN table_name = 'geo_features' THEN 'DEFAULT' " +
                    "ELSE 'CUSTOM' " +
                "END as table_type " +
                "FROM gis_manage_template " +
                "WHERE in_or_out = 'in' " +
                "ORDER BY id";
            
            List<Map<String, Object>> templates = jdbcTemplate.queryForList(sql);
            
            response.put("success", true);
            response.put("templates", templates);
            response.put("count", templates.size());
            
            log.info("获取到 {} 个可用模板", templates.size());
            
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查指定模板的详细信息
     */
    @GetMapping("/template/{templateId}/detail")
    public ResponseEntity<Map<String, Object>> getTemplateDetail(@PathVariable Integer templateId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 直接查询数据库获取模板详细信息
            String sql = "SELECT " +
                "id, " +
                "table_name, " +
                "name_zh, " +
                "name_en, " +
                "original_coordinate_system, " +
                "target_coordinate_system, " +
                "is_zh, " +
                "type, " +
                "template_type, " +
                "data_base, " +
                "in_or_out, " +
                "map, " +
                "create_time " +
                "FROM gis_manage_template " +
                "WHERE id = ?";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, templateId);
            
            if (results.isEmpty()) {
                response.put("success", false);
                response.put("message", "模板ID " + templateId + " 不存在");
            } else {
                Map<String, Object> template = results.get(0);
                
                // 检查关键字段
                Map<String, Object> validation = new HashMap<>();
                validation.put("hasTableName", template.get("table_name") != null && !template.get("table_name").toString().trim().isEmpty());
                validation.put("hasNameZh", template.get("name_zh") != null && !template.get("name_zh").toString().trim().isEmpty());
                validation.put("hasMapJson", template.get("map") != null && !template.get("map").toString().trim().isEmpty());
                validation.put("hasCoordinateSystem", template.get("original_coordinate_system") != null && !template.get("original_coordinate_system").toString().trim().isEmpty());
                
                // 判断是否使用自定义表
                String tableName = (String) template.get("table_name");
                boolean isCustomTable = tableName != null && !tableName.trim().isEmpty() && !"geo_features".equals(tableName);
                
                response.put("success", true);
                response.put("template", template);
                response.put("validation", validation);
                response.put("isCustomTable", isCustomTable);
                response.put("targetTable", isCustomTable ? tableName : "geo_features");
            }
            
        } catch (Exception e) {
            log.error("获取模板详细信息失败，模板ID: {}", templateId, e);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查目标表是否存在
     */
    @GetMapping("/check-table/{tableName}")
    public ResponseEntity<Map<String, Object>> checkTable(@PathVariable String tableName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            
            boolean exists = count != null && count > 0;
            
            if (exists) {
                // 获取表结构信息
                String columnSql = "SELECT column_name, data_type, is_nullable " +
                    "FROM information_schema.columns " +
                    "WHERE table_name = ? " +
                    "ORDER BY ordinal_position";
                List<Map<String, Object>> columns = jdbcTemplate.queryForList(columnSql, tableName);
                
                response.put("exists", true);
                response.put("columns", columns);
                response.put("message", "表 " + tableName + " 存在，包含 " + columns.size() + " 个字段");
            } else {
                response.put("exists", false);
                response.put("message", "表 " + tableName + " 不存在");
            }
            
            response.put("success", true);
            response.put("tableName", tableName);
            
        } catch (Exception e) {
            log.error("检查表失败: {}", tableName, e);
            response.put("success", false);
            response.put("message", "检查失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
