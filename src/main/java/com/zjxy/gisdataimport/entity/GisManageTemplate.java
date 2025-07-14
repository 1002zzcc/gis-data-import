package com.zjxy.gisdataimport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * GIS数据导入模板实体类
 * 用于配置Shapefile数据导入的字段映射、坐标转换等规则
 */
@Data
@TableName("gis_manage_template")
public class GisManageTemplate {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 关联的数据库表ID
     */
    private Integer tableId;

    /**
     * 目标数据库表名
     */
    private String tableName;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 模板中文名称
     */
    private String nameZh;

    /**
     * 模板英文名称
     */
    private String nameEn;

    /**
     * 表数据开始行号（Excel导入时使用）
     */
    private Integer thLine;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 线坐标来源：1来源于数据库，2来源于excel表
     */
    private Integer lineType;

    /**
     * 是否进行坐标转换
     */
    private Boolean isZh;

    /**
     * 模板类型：1纯文本，2点表，3线表
     */
    private Integer type;

    /**
     * 源坐标系
     */
    private String originalCoordinateSystem;

    /**
     * 目标坐标系
     */
    private String targetCoordinateSystem;

    /**
     * 线要素映射配置（JSON格式存储）
     */
    @TableField("line_map")
    private String lineMapJson;

    /**
     * 点要素映射配置（JSON格式存储）
     */
    @TableField("point_map")
    private String pointMapJson;

    /**
     * 字段映射配置（JSON格式存储）
     */
    @TableField("map")
    private String mapJson;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 模板类型：excel, shp等
     */
    private String templateType;

    /**
     * 数据库名称
     */
    private String dataBase;

    /**
     * 数据库模式
     */
    private String dataBaseMode;

    /**
     * 数据库表名
     */
    private String dataBaseTable;

    /**
     * 是否图示分类
     */
    private Boolean tsfl;

    /**
     * 是否图形多表
     */
    private Boolean txdb;

    /**
     * 用户ID
     */
    private String uid;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 分组名称
     */
    private String groups;

    /**
     * Sheet名称（Excel导入时使用）
     */
    private String sheetName;

    /**
     * 是否启用校验规则
     */
    private Boolean checkRule;

    /**
     * 校验规则ID
     */
    private Integer checkRuleId;

    /**
     * 导入导出标识：in导入，out导出
     */
    private String inOrOut;

    /**
     * 值域映射配置（JSON格式存储）
     */
    @TableField("value_map")
    private String valueMapJson;

    /**
     * 图层英文名称
     */
    private String layerEn;

    /**
     * 关联配置（JSON格式存储）
     */
    @TableField("association")
    private String associationJson;

    // ========== 非数据库字段，用于业务处理 ==========

    @TableField(exist = false)
    private Integer field;

    @TableField(exist = false)
    private String value;

    @TableField(exist = false)
    private String fields;

    // ========== JSON字段的Java对象转换方法 ==========

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取字段映射配置
     */
    public List<Map<String, Object>> getMap() {
        if (mapJson == null || mapJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(mapJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new RuntimeException("解析字段映射配置失败", e);
        }
    }

    /**
     * 设置字段映射配置
     */
    public void setMap(List<Map<String, Object>> map) {
        if (map == null) {
            this.mapJson = null;
            return;
        }
        try {
            this.mapJson = objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("序列化字段映射配置失败", e);
        }
    }

    /**
     * 获取线要素映射配置
     */
    public Map<String, Object> getLineMap() {
        if (lineMapJson == null || lineMapJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(lineMapJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("解析线要素映射配置失败", e);
        }
    }

    /**
     * 设置线要素映射配置
     */
    public void setLineMap(Map<String, Object> lineMap) {
        if (lineMap == null) {
            this.lineMapJson = null;
            return;
        }
        try {
            this.lineMapJson = objectMapper.writeValueAsString(lineMap);
        } catch (Exception e) {
            throw new RuntimeException("序列化线要素映射配置失败", e);
        }
    }

    /**
     * 获取点要素映射配置
     */
    public Map<String, Object> getPointMap() {
        if (pointMapJson == null || pointMapJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(pointMapJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("解析点要素映射配置失败", e);
        }
    }

    /**
     * 设置点要素映射配置
     */
    public void setPointMap(Map<String, Object> pointMap) {
        if (pointMap == null) {
            this.pointMapJson = null;
            return;
        }
        try {
            this.pointMapJson = objectMapper.writeValueAsString(pointMap);
        } catch (Exception e) {
            throw new RuntimeException("序列化点要素映射配置失败", e);
        }
    }

    /**
     * 获取值域映射配置
     */
    public List<Map<String, Object>> getValueMap() {
        if (valueMapJson == null || valueMapJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(valueMapJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new RuntimeException("解析值域映射配置失败", e);
        }
    }

    /**
     * 设置值域映射配置
     */
    public void setValueMap(List<Map<String, Object>> valueMap) {
        if (valueMap == null) {
            this.valueMapJson = null;
            return;
        }
        try {
            this.valueMapJson = objectMapper.writeValueAsString(valueMap);
        } catch (Exception e) {
            throw new RuntimeException("序列化值域映射配置失败", e);
        }
    }

    /**
     * 获取关联配置
     */
    public Map<String, Object> getAssociation() {
        if (associationJson == null || associationJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(associationJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("解析关联配置失败", e);
        }
    }

    /**
     * 设置关联配置
     */
    public void setAssociation(Map<String, Object> association) {
        if (association == null) {
            this.associationJson = null;
            return;
        }
        try {
            this.associationJson = objectMapper.writeValueAsString(association);
        } catch (Exception e) {
            throw new RuntimeException("序列化关联配置失败", e);
        }
    }
}
