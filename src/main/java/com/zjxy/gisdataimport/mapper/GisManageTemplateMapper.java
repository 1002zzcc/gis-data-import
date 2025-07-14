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
    @Select("SELECT * FROM gis_manage_template WHERE id = #{templateId}")
    GisManageTemplate selectByTemplateId(@Param("templateId") Integer templateId);

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
}
