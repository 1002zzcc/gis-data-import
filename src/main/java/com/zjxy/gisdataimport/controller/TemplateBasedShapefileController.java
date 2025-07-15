package com.zjxy.gisdataimport.controller;

import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.service.BatchInsertService;
import com.zjxy.gisdataimport.service.GisManageTemplateService;
import com.zjxy.gisdataimport.service.TemplateBasedShapefileService;
import com.zjxy.gisdataimport.shap.ShapefileReader;
import lombok.extern.slf4j.Slf4j;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 基于模板的Shapefile处理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/template-shapefile")
public class TemplateBasedShapefileController {

    @Autowired
    private TemplateBasedShapefileService templateBasedShapefileService;

    @Autowired
    private ShapefileReader shapefileReader;

    @Autowired
    private BatchInsertService batchInsertService;

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
            Map<String, Object> result = templateBasedShapefileService.processShapefileWithTemplate(
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
            Map<String, Object> result = templateBasedShapefileService.processShapefileWithTemplateFromPath(filePath, templateId);

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

            // 添加调试日志，检查JSON字段是否为空
            log.info("模板详情 - ID: {}, 名称: {}", template.getId(), template.getNameZh());
            log.info("JSON字段检查:");
            log.info("  mapJson: {}", template.getMapJson() != null ? "有数据" : "为空");
            log.info("  lineMapJson: {}", template.getLineMapJson() != null ? "有数据" : "为空");
            log.info("  pointMapJson: {}", template.getPointMapJson() != null ? "有数据" : "为空");
            log.info("  valueMapJson: {}", template.getValueMapJson() != null ? "有数据" : "为空");
            log.info("  associationJson: {}", template.getAssociationJson() != null ? "有数据" : "为空");

            if (template.getMapJson() != null) {
                log.info("  mapJson内容: {}", template.getMapJson());
            }

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

    /*
     * 调试方法：详细检查模板数据
     */
    /*
    @GetMapping("/templates/{templateId}/debug")
    public ResponseEntity<Map<String, Object>> debugTemplateById(@PathVariable Integer templateId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 使用调试方法获取模板
            GisManageTemplate template = templateService.getTemplateByIdWithDebug(templateId);

            // 详细调试信息
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("templateId", template.getId());
            debugInfo.put("nameZh", template.getNameZh());
            debugInfo.put("tableName", template.getTableName());
            debugInfo.put("templateType", template.getTemplateType());

            // 检查JSON字段的原始值
            debugInfo.put("mapJson_raw", template.getMapJson());
            debugInfo.put("lineMapJson_raw", template.getLineMapJson());
            debugInfo.put("pointMapJson_raw", template.getPointMapJson());
            debugInfo.put("valueMapJson_raw", template.getValueMapJson());
            debugInfo.put("associationJson_raw", template.getAssociationJson());

            // 检查JSON字段是否为null
            debugInfo.put("mapJson_isNull", template.getMapJson() == null);
            debugInfo.put("lineMapJson_isNull", template.getLineMapJson() == null);
            debugInfo.put("pointMapJson_isNull", template.getPointMapJson() == null);
            debugInfo.put("valueMapJson_isNull", template.getValueMapJson() == null);
            debugInfo.put("associationJson_isNull", template.getAssociationJson() == null);

            // 尝试解析JSON字段
            try {
                debugInfo.put("map_parsed", template.getMap());
            } catch (Exception e) {
                debugInfo.put("map_parseError", e.getMessage());
            }

            try {
                debugInfo.put("lineMap_parsed", template.getLineMap());
            } catch (Exception e) {
                debugInfo.put("lineMap_parseError", e.getMessage());
            }

            try {
                debugInfo.put("pointMap_parsed", template.getPointMap());
            } catch (Exception e) {
                debugInfo.put("pointMap_parseError", e.getMessage());
            }

            try {
                debugInfo.put("valueMap_parsed", template.getValueMap());
            } catch (Exception e) {
                debugInfo.put("valueMap_parseError", e.getMessage());
            }

            try {
                debugInfo.put("association_parsed", template.getAssociation());
            } catch (Exception e) {
                debugInfo.put("association_parseError", e.getMessage());
            }

            response.put("success", true);
            response.put("debugInfo", debugInfo);
            response.put("fullTemplate", template);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("调试模板详情失败，模板ID: {}", templateId, e);
            response.put("success", false);
            response.put("message", "调试模板详情失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    */

    /*
     * 简单测试方法：检查数据库连接和基本查询
     */
    /*
    @GetMapping("/templates/test-connection")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取所有模板列表
            List<GisManageTemplate> templates = templateService.getAllImportTemplates();

            Map<String, Object> testInfo = new HashMap<>();
            testInfo.put("totalTemplates", templates.size());

            if (!templates.isEmpty()) {
                GisManageTemplate firstTemplate = templates.get(0);
                testInfo.put("firstTemplateId", firstTemplate.getId());
                testInfo.put("firstTemplateName", firstTemplate.getNameZh());
                testInfo.put("firstTemplateTableName", firstTemplate.getTableName());

                // 检查第一个模板的JSON字段
                testInfo.put("firstTemplate_mapJson_exists", firstTemplate.getMapJson() != null);
                testInfo.put("firstTemplate_lineMapJson_exists", firstTemplate.getLineMapJson() != null);
                testInfo.put("firstTemplate_pointMapJson_exists", firstTemplate.getPointMapJson() != null);
                testInfo.put("firstTemplate_valueMapJson_exists", firstTemplate.getValueMapJson() != null);
                testInfo.put("firstTemplate_associationJson_exists", firstTemplate.getAssociationJson() != null);

                if (firstTemplate.getMapJson() != null) {
                    testInfo.put("firstTemplate_mapJson_length", firstTemplate.getMapJson().length());
                    testInfo.put("firstTemplate_mapJson_preview",
                        firstTemplate.getMapJson().length() > 50 ?
                            firstTemplate.getMapJson().substring(0, 50) + "..." :
                            firstTemplate.getMapJson());
                }
            }

            response.put("success", true);
            response.put("testInfo", testInfo);
            response.put("message", "数据库连接正常，查询到 " + templates.size() + " 个模板");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
            response.put("success", false);
            response.put("message", "数据库连接测试失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    */

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
     * 使用模板导入Shapefile到数据库（完整流程演示）
     */
    @PostMapping("/import-with-template")
    public ResponseEntity<Map<String, Object>> importShapefileWithTemplate(
            @RequestParam("shapefilePath") String shapefilePath,
            @RequestParam("templateId") Integer templateId) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("开始使用模板导入Shapefile: 文件={}, 模板ID={}", shapefilePath, templateId);

            // 1. 获取模板配置
            GisManageTemplate template = templateService.getTemplateById(templateId);
            if (template == null) {
                response.put("success", false);
                response.put("message", "模板不存在: " + templateId);
                return ResponseEntity.badRequest().body(response);
            }

            // 2. 读取Shapefile
            List<SimpleFeature> features = shapefileReader.readFeatures(shapefilePath);
            SimpleFeatureType schema = shapefileReader.getSchema(shapefilePath);

            log.info("读取Shapefile成功: 要素数量={}, 字段数量={}", features.size(), schema.getAttributeCount());

            // 3. 显示字段映射信息
            Map<String, String> fieldMapping = templateBasedShapefileService.getFieldMappingFromTemplate(template);
            Map<String, Object> tableInfo = templateBasedShapefileService.getTargetTableInfoFromTemplate(template);

            // 4. 批量转换数据
            List<GeoFeatureEntity> entities = templateBasedShapefileService.batchProcessFeaturesWithTemplate(
                features, schema, template);

            log.info("数据转换完成: 转换成功={} 条记录", entities.size());

            // 5. 插入数据库
            long insertStartTime = System.currentTimeMillis();

            // 这里演示两种插入方式
            Map<String, Object> insertResult;
            if (template.getTableName() != null && !"geo_features".equals(template.getTableName())) {
                // 插入到自定义表（需要实现动态SQL）
                insertResult = insertToCustomTable(entities, template, fieldMapping);
            } else {
                // 插入到默认表
                insertResult = insertToDefaultTable(entities);
            }

            long insertEndTime = System.currentTimeMillis();
            long insertTime = insertEndTime - insertStartTime;

            // 6. 构建响应
            response.put("success", true);

            // 构建导入摘要
            Map<String, Object> importSummary = new HashMap<>();
            importSummary.put("shapefilePath", shapefilePath);
            importSummary.put("templateId", templateId);
            importSummary.put("templateName", template.getNameZh());
            importSummary.put("totalFeatures", features.size());
            importSummary.put("convertedEntities", entities.size());
            importSummary.put("insertedRecords", insertResult.get("insertedCount"));
            importSummary.put("insertTime", insertTime + "ms");

            response.put("importSummary", importSummary);
            response.put("fieldMapping", fieldMapping);
            response.put("targetTable", tableInfo);
            response.put("insertResult", insertResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("使用模板导入Shapefile失败", e);
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 插入到默认表的示例方法
     */
    private Map<String, Object> insertToDefaultTable(List<GeoFeatureEntity> entities) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 使用现有的批量插入服务
            batchInsertService.fastBatchInsert(entities);

            result.put("success", true);
            result.put("insertedCount", entities.size());
            result.put("tableName", "geo_features");
            result.put("method", "MyBatis-Plus批量插入");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "插入失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 插入到自定义表的示例方法
     */
    private Map<String, Object> insertToCustomTable(List<GeoFeatureEntity> entities,
                                                   GisManageTemplate template,
                                                   Map<String, String> fieldMapping) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 构建动态SQL示例
            String tableName = template.getTableName();
            List<String> dbFields = new ArrayList<>(fieldMapping.values());

            // 添加几何字段
            if (!dbFields.contains("geom")) {
                dbFields.add("geom");
            }

            // 构建INSERT SQL
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ").append(tableName).append(" (");
            sql.append(String.join(", ", dbFields));
            sql.append(") VALUES (");
            sql.append(String.join(", ", Collections.nCopies(dbFields.size(), "?")));
            sql.append(")");

            result.put("success", true);
            result.put("insertedCount", entities.size());
            result.put("tableName", tableName);
            result.put("method", "动态SQL批量插入");
            result.put("sql", sql.toString());
            result.put("fields", dbFields);
            result.put("note", "这是SQL构建示例，实际执行需要完整的JDBC实现");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "构建动态SQL失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 根据模板生成CREATE TABLE SQL语句
     */
    @GetMapping("/templates/{templateId}/generate-sql")
    public ResponseEntity<Map<String, Object>> generateCreateTableSQL(@PathVariable Integer templateId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("开始为模板生成CREATE TABLE SQL，模板ID: {}", templateId);

            // 1. 获取模板配置
            GisManageTemplate template = templateService.getTemplateById(templateId);
            if (template == null) {
                response.put("success", false);
                response.put("message", "模板不存在: " + templateId);
                return ResponseEntity.badRequest().body(response);
            }

            // 2. 检查模板配置
            if (template.getTableName() == null || template.getTableName().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "模板中未配置目标表名");
                return ResponseEntity.badRequest().body(response);
            }

            if (template.getMap() == null || template.getMap().isEmpty()) {
                response.put("success", false);
                response.put("message", "模板中没有字段映射配置");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. 生成SQL语句
            String createTableSQL = generateCreateTableSQLFromTemplate(template);

            // 4. 分析字段配置
            Map<String, Object> fieldAnalysis = analyzeTemplateFields(template);

            // 5. 构建响应
            response.put("success", true);
            response.put("templateId", templateId);
            response.put("templateName", template.getNameZh());
            response.put("tableName", template.getTableName());
            response.put("schema", template.getDataBaseMode());
            response.put("createTableSQL", createTableSQL);
            response.put("fieldAnalysis", fieldAnalysis);
            response.put("message", "CREATE TABLE SQL生成成功");

            log.info("CREATE TABLE SQL生成成功，模板ID: {}, 表名: {}", templateId, template.getTableName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("生成CREATE TABLE SQL失败", e);
            response.put("success", false);
            response.put("message", "生成SQL失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 生成CREATE TABLE SQL（简化版本，用于控制器）
     */
    private String generateCreateTableSQLFromTemplate(GisManageTemplate template) {
        String tableName = template.getTableName();
        String schema = template.getDataBaseMode();

        // 构建完整表名
        String fullTableName = (schema != null && !schema.trim().isEmpty() && !"public".equals(schema.trim()))
            ? schema + "." + tableName : tableName;

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(fullTableName).append(" (\n");
        sql.append("    id SERIAL PRIMARY KEY,\n");

        // 处理字段映射
        List<Map<String, Object>> mapConfig = template.getMap();
        List<String> fieldDefinitions = new ArrayList<>();
        boolean hasGeometryField = false;
        String geometryFieldName = null;

        for (Map<String, Object> fieldConfig : mapConfig) {
            Boolean checked = (Boolean) fieldConfig.get("checked");
            if (checked != null && checked) {
                String fieldName = (String) fieldConfig.get("fieldName");
                String fieldType = (String) fieldConfig.get("fieldType");
                String shpFieldType = (String) fieldConfig.get("shpFieldType");

                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    String pgType = mapFieldTypeToPostgreSQL(fieldType, shpFieldType);
                    fieldDefinitions.add(fieldName + " " + pgType);

                    if (isGeometryFieldType(fieldType, shpFieldType)) {
                        hasGeometryField = true;
                        geometryFieldName = fieldName;
                    }
                }
            }
        }

        // 添加字段定义
        for (int i = 0; i < fieldDefinitions.size(); i++) {
            sql.append("    ").append(fieldDefinitions.get(i));
            if (i < fieldDefinitions.size() - 1) {
                sql.append(",");
            }
            sql.append("\n");
        }

        sql.append(");\n");

        // 添加空间索引
        if (hasGeometryField && geometryFieldName != null) {
            sql.append("\n-- 创建空间索引\n");
            sql.append("CREATE INDEX idx_").append(tableName).append("_").append(geometryFieldName)
               .append(" ON ").append(fullTableName).append(" USING GIST (").append(geometryFieldName).append(");\n");
        }

        // 添加表注释
        sql.append("\n-- 表注释\n");
        sql.append("COMMENT ON TABLE ").append(fullTableName).append(" IS '")
           .append("模板生成: ").append(template.getNameZh() != null ? template.getNameZh() : tableName)
           .append("';\n");

        return sql.toString();
    }

    /**
     * 分析模板字段配置
     */
    private Map<String, Object> analyzeTemplateFields(GisManageTemplate template) {
        Map<String, Object> analysis = new HashMap<>();

        List<Map<String, Object>> mapConfig = template.getMap();
        int totalFields = mapConfig.size();
        int checkedFields = 0;
        int geometryFields = 0;
        int stringFields = 0;
        int numericFields = 0;

        List<Map<String, Object>> selectedFields = new ArrayList<>();

        for (Map<String, Object> fieldConfig : mapConfig) {
            Boolean checked = (Boolean) fieldConfig.get("checked");
            if (checked != null && checked) {
                checkedFields++;
                selectedFields.add(fieldConfig);

                String fieldType = (String) fieldConfig.get("fieldType");
                String shpFieldType = (String) fieldConfig.get("shpFieldType");

                if (isGeometryFieldType(fieldType, shpFieldType)) {
                    geometryFields++;
                } else if (isStringFieldType(fieldType)) {
                    stringFields++;
                } else if (isNumericFieldType(fieldType)) {
                    numericFields++;
                }
            }
        }

        analysis.put("totalFields", totalFields);
        analysis.put("checkedFields", checkedFields);
        analysis.put("geometryFields", geometryFields);
        analysis.put("stringFields", stringFields);
        analysis.put("numericFields", numericFields);
        analysis.put("selectedFields", selectedFields);

        return analysis;
    }

    /**
     * 映射字段类型到PostgreSQL类型
     */
    private String mapFieldTypeToPostgreSQL(String fieldType, String shpFieldType) {
        if (fieldType == null) fieldType = "";
        String lowerFieldType = fieldType.toLowerCase();

        if (isGeometryFieldType(fieldType, shpFieldType)) {
            if (shpFieldType != null) {
                String lowerShpType = shpFieldType.toLowerCase();
                if (lowerShpType.contains("point")) return "GEOMETRY(POINT, 4326)";
                if (lowerShpType.contains("line")) return "GEOMETRY(LINESTRING, 4326)";
                if (lowerShpType.contains("polygon")) return "GEOMETRY(POLYGON, 4326)";
            }
            return "GEOMETRY";
        }

        switch (lowerFieldType) {
            case "integer": case "int": case "int4": return "INTEGER";
            case "bigint": case "int8": return "BIGINT";
            case "double precision": case "float8": return "DOUBLE PRECISION";
            case "real": case "float4": return "REAL";
            case "numeric": case "decimal": return "NUMERIC";
            case "boolean": case "bool": return "BOOLEAN";
            case "date": return "DATE";
            case "timestamp": return "TIMESTAMP";
            case "character varying": case "varchar": return "VARCHAR(255)";
            case "text": return "TEXT";
            default: return "VARCHAR(255)";
        }
    }

    /**
     * 检查是否为几何字段类型
     */
    private boolean isGeometryFieldType(String fieldType, String shpFieldType) {
        return isGeometryType(fieldType) || isGeometryType(shpFieldType);
    }

    private boolean isGeometryType(String type) {
        if (type == null) return false;
        String lowerType = type.toLowerCase();
        return lowerType.contains("point") || lowerType.contains("line") ||
               lowerType.contains("polygon") || lowerType.contains("geometry");
    }

    private boolean isStringFieldType(String fieldType) {
        if (fieldType == null) return false;
        String lowerType = fieldType.toLowerCase();
        return lowerType.contains("varchar") || lowerType.contains("text") ||
               lowerType.contains("char") || lowerType.contains("string");
    }

    private boolean isNumericFieldType(String fieldType) {
        if (fieldType == null) return false;
        String lowerType = fieldType.toLowerCase();
        return lowerType.contains("int") || lowerType.contains("double") ||
               lowerType.contains("float") || lowerType.contains("numeric") ||
               lowerType.contains("decimal") || lowerType.contains("real");
    }

    /**
     * 创建默认的 geo_features 表
     */
    @PostMapping("/create-default-table")
    public ResponseEntity<Map<String, Object>> createDefaultGeoFeaturesTable() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("开始创建默认的 geo_features 表");

            // 生成创建默认表的SQL
            String createTableSQL = generateDefaultGeoFeaturesTableSQL();

            // 这里可以选择自动执行或者返回SQL让用户手动执行
            response.put("success", true);
            response.put("tableName", "geo_features");
            response.put("createTableSQL", createTableSQL);
            response.put("message", "默认表SQL生成成功，请手动执行以下SQL语句");
            response.put("instructions", "复制SQL语句到PostgreSQL数据库中执行，然后重试导入操作");

            log.info("默认 geo_features 表SQL生成成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("创建默认表SQL失败", e);
            response.put("success", false);
            response.put("message", "生成默认表SQL失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 生成默认 geo_features 表的SQL
     */
    private String generateDefaultGeoFeaturesTableSQL() {
        StringBuilder sql = new StringBuilder();

        sql.append("-- 创建默认的 geo_features 表\n");
        sql.append("-- 这是系统的默认表，用于存储通用的地理要素数据\n\n");

        sql.append("-- 1. 创建表结构\n");
        sql.append("CREATE TABLE IF NOT EXISTS \"public\".\"geo_features\" (\n");
        sql.append("    \"id\" SERIAL PRIMARY KEY,\n");
        sql.append("    \"feature_id\" VARCHAR(255),\n");
        sql.append("    \"geometry\" GEOMETRY,\n");
        sql.append("    \"attributes\" TEXT,\n");
        sql.append("    \"created_at\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n");
        sql.append("    \"updated_at\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n");
        sql.append(");\n\n");

        sql.append("-- 2. 创建空间索引\n");
        sql.append("CREATE INDEX IF NOT EXISTS \"idx_geo_features_geometry\"\n");
        sql.append("    ON \"public\".\"geo_features\" USING GIST (\"geometry\");\n\n");

        sql.append("-- 3. 创建其他索引\n");
        sql.append("CREATE INDEX IF NOT EXISTS \"idx_geo_features_feature_id\"\n");
        sql.append("    ON \"public\".\"geo_features\" (\"feature_id\");\n\n");

        sql.append("CREATE INDEX IF NOT EXISTS \"idx_geo_features_created_at\"\n");
        sql.append("    ON \"public\".\"geo_features\" (\"created_at\");\n\n");

        sql.append("-- 4. 表注释\n");
        sql.append("COMMENT ON TABLE \"public\".\"geo_features\" IS '通用地理要素表 - 系统默认表';\n\n");

        sql.append("-- 5. 字段注释\n");
        sql.append("COMMENT ON COLUMN \"public\".\"geo_features\".\"id\" IS '主键ID';\n");
        sql.append("COMMENT ON COLUMN \"public\".\"geo_features\".\"feature_id\" IS '要素ID（来自Shapefile）';\n");
        sql.append("COMMENT ON COLUMN \"public\".\"geo_features\".\"geometry\" IS '几何数据（支持所有几何类型）';\n");
        sql.append("COMMENT ON COLUMN \"public\".\"geo_features\".\"attributes\" IS '属性数据（JSON格式）';\n");
        sql.append("COMMENT ON COLUMN \"public\".\"geo_features\".\"created_at\" IS '创建时间';\n");
        sql.append("COMMENT ON COLUMN \"public\".\"geo_features\".\"updated_at\" IS '更新时间';\n\n");

        sql.append("-- 6. 创建更新时间触发器\n");
        sql.append("CREATE OR REPLACE FUNCTION update_updated_at_column()\n");
        sql.append("RETURNS TRIGGER AS $$\n");
        sql.append("BEGIN\n");
        sql.append("    NEW.updated_at = CURRENT_TIMESTAMP;\n");
        sql.append("    RETURN NEW;\n");
        sql.append("END;\n");
        sql.append("$$ language 'plpgsql';\n\n");

        sql.append("CREATE TRIGGER update_geo_features_updated_at \n");
        sql.append("    BEFORE UPDATE ON \"public\".\"geo_features\" \n");
        sql.append("    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();\n\n");

        sql.append("-- 7. 验证表创建\n");
        sql.append("SELECT 'geo_features表创建成功！' AS message;\n");
        sql.append("SELECT COUNT(*) AS total_records FROM \"public\".\"geo_features\";\n");

        return sql.toString();
    }
}
