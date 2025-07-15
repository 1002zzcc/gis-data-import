package com.zjxy.gisdataimport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjxy.gisdataimport.entity.GisManageTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * GIS管理模板Mapper接口
 */
@Mapper
public interface GisManageTemplateMapper extends BaseMapper<GisManageTemplate> {

    /**
     * 根据模板ID查询模板信息
     */
    @Select("SELECT * FROM gis_manage_template WHERE id = #{id}")
    GisManageTemplate selectByTemplateId(@Param("id") Integer id);

    /**
     * 根据表名查询模板列表
     */
    @Select("SELECT * FROM gis_manage_template WHERE table_name = #{tableName} AND in_or_out = 'in'")
    List<GisManageTemplate> selectByTableName(@Param("tableName") String tableName);

    /**
     * 根据分组名称查询模板列表
     */
    @Select("SELECT * FROM gis_manage_template WHERE groups = #{groups} AND data_base = #{dataBase} AND in_or_out = #{inOrOut}")
    List<GisManageTemplate> selectByGroups(@Param("groups") String groups,
                                          @Param("dataBase") String dataBase,
                                          @Param("inOrOut") String inOrOut);

    /**
     * 根据模板类型查询模板列表
     */
    @Select("SELECT * FROM gis_manage_template WHERE template_type = #{templateType} AND in_or_out = 'in'")
    List<GisManageTemplate> selectByTemplateType(@Param("templateType") String templateType);

    /**
     * 根据几何类型查询模板列表
     */
    @Select("SELECT * FROM gis_manage_template WHERE type = #{type} AND in_or_out = 'in'")
    List<GisManageTemplate> selectByGeometryType(@Param("type") Integer type);

    /**
     * 查询所有导入模板
     */
    @Select("SELECT * FROM gis_manage_template WHERE in_or_out = 'in' ORDER BY create_time DESC")
    List<GisManageTemplate> selectAllImportTemplates();

    /**
     * 根据Sheet名称和分组查询模板
     */
    @Select("SELECT * FROM gis_manage_template WHERE sheet_name = #{sheetName} AND groups = #{groups} AND data_base = #{dataBase}")
    GisManageTemplate selectBySheetNameAndGroups(@Param("sheetName") String sheetName,
                                                @Param("groups") String groups,
                                                @Param("dataBase") String dataBase);

    /*
     * 调试方法：查询模板的原始数据，包括JSON字段的详细信息
     */
    /*
    @Select("SELECT id, table_id, table_name, datasource_name, name_zh, name_en, " +
            "th_line, file_path, line_type, is_zh, type, " +
            "original_coordinate_system, target_coordinate_system, " +
            "map as mapJson, line_map as lineMapJson, point_map as pointMapJson, " +
            "value_map as valueMapJson, association as associationJson, " +
            "create_time, template_type, data_base, data_base_mode, data_base_table, " +
            "tsfl, txdb, uid, app_id, groups, sheet_name, check_rule, check_rule_id, " +
            "in_or_out, layer_en " +
            "FROM gis_manage_template WHERE id = #{id}")
    GisManageTemplate selectByIdWithDebug(@Param("id") Integer id);
    */
}
