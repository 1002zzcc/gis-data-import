package com.zjxy.gisdataimport.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjxy.gisdataimport.entity.GisManageTemplate;

import java.util.List;
import java.util.Map;

/**
 * GIS管理模板服务接口
 */
public interface GisManageTemplateService extends IService<GisManageTemplate> {

    /**
     * 根据模板ID获取模板
     *
     * @param templateId 模板ID
     * @return 模板信息
     */
    GisManageTemplate getTemplateById(Integer templateId);

    /**
     * 根据表名获取模板列表
     *
     * @param tableName 表名
     * @return 模板列表
     */
    List<GisManageTemplate> getTemplatesByTableName(String tableName);

    /**
     * 根据分组获取模板列表
     *
     * @param groups 分组名称
     * @param dataBase 数据库名
     * @param inOrOut 导入/导出类型
     * @return 模板列表
     */
    List<GisManageTemplate> getTemplatesByGroups(String groups, String dataBase, String inOrOut);

    /**
     * 根据模板类型获取模板列表
     *
     * @param templateType 模板类型
     * @return 模板列表
     */
    List<GisManageTemplate> getTemplatesByType(String templateType);

    /**
     * 根据几何类型获取模板列表
     *
     * @param geometryType 几何类型
     * @return 模板列表
     */
    List<GisManageTemplate> getTemplatesByGeometryType(Integer geometryType);

    /**
     * 获取所有导入模板
     *
     * @return 模板列表
     */
    List<GisManageTemplate> getAllImportTemplates();

    /**
     * 根据Sheet名称和分组获取模板
     *
     * @param sheetName Sheet名称
     * @param groups 分组名称
     * @param dataBase 数据库名
     * @return 模板信息
     */
    GisManageTemplate getTemplateBySheetNameAndGroups(String sheetName, String groups, String dataBase);

    /**
     * 创建模板
     *
     * @param template 模板信息
     * @return 模板ID
     */
    Integer createTemplate(GisManageTemplate template);

    /**
     * 更新模板
     *
     * @param template 模板信息
     * @return 更新结果
     */
    Boolean updateTemplate(GisManageTemplate template);

    /**
     * 删除模板
     *
     * @param templateId 模板ID
     * @return 删除结果
     */
    Boolean deleteTemplate(Integer templateId);

    /**
     * 验证模板配置
     *
     * @param template 模板信息
     * @return 验证结果
     */
    Boolean validateTemplate(GisManageTemplate template);
    
    /**
     * 分页查询模板
     *
     * @param id 模板ID
     * @param tableName 表名
     * @param nameZh 中文名称
     * @param dataBase 数据库名
     * @param pageSize 分页大小
     * @param pageIndex 页码
     * @return 模板分页结果
     */
    Page<GisManageTemplate> getTemplatesPage(Integer id, String tableName, String nameZh, String dataBase, Long pageSize, Long pageIndex);
    
    /**
     * 根据数据库名称获取模板列表
     *
     * @param database 数据库名称
     * @return 模板列表
     */
    List<GisManageTemplate> getTemplatesByDatabase(String database);
    
    /**
     * 根据应用ID和分组获取模板列表
     *
     * @param appId 应用ID
     * @param groups 分组名称
     * @param templateType 模板类型
     * @param inOrOut 导入/导出类型
     * @return 模板列表
     */
    List<GisManageTemplate> getTemplatesByAppIdAndGroups(String appId, String groups, String templateType, String inOrOut);
}
