package com.zjxy.gisdataimport.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjxy.gisdataimport.entity.GisManageTemplate;
import com.zjxy.gisdataimport.mapper.GisManageTemplateMapper;
import com.zjxy.gisdataimport.service.GisManageTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GIS管理模板服务实现类
 */
@Slf4j
@Service
public class GisManageTemplateServiceImpl extends ServiceImpl<GisManageTemplateMapper, GisManageTemplate>
        implements GisManageTemplateService {

    @Resource
    private GisManageTemplateMapper templateMapper;

    @Value("${remove.table:}")
    private String removeTable;

    @Value("${remove.field:}")
    private String removeField;

    @Override
    public GisManageTemplate getTemplateById(Integer templateId) {
        if (templateId == null) {
            throw new IllegalArgumentException("模板ID不能为空");
        }

        GisManageTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("未找到ID为 " + templateId + " 的模板");
        }

        // 详细调试日志
        log.info("=== 模板详细信息调试 ===");
        log.info("模板ID: {}", template.getId());
        log.info("模板名称: {}", template.getNameZh());
        log.info("目标表名: {}", template.getTableName());
        log.info("源坐标系: {}", template.getOriginalCoordinateSystem());
        log.info("目标坐标系: {}", template.getTargetCoordinateSystem());
        log.info("坐标转换: {}", template.getIsZh());
        log.info("模板类型: {}", template.getType());
        log.info("数据库名: {}", template.getDataBase());
        log.info("字段映射JSON: {}", template.getMapJson());
        log.info("是否使用自定义表: {}", template.getTableName() != null &&
                !template.getTableName().trim().isEmpty() &&
                !"geo_features".equals(template.getTableName()));
        log.info("=== 模板调试信息结束 ===");

        return template;
    }

    @Override
    public List<GisManageTemplate> getTemplatesByTableName(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }

        List<GisManageTemplate> templates = templateMapper.selectByTableName(tableName);
        log.info("根据表名 {} 查询到 {} 个模板", tableName, templates.size());
        return templates;
    }

    @Override
    public List<GisManageTemplate> getTemplatesByGroups(String groups, String dataBase, String inOrOut) {
        if (!StringUtils.hasText(groups)) {
            throw new IllegalArgumentException("分组名称不能为空");
        }

        QueryWrapper<GisManageTemplate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("groups", groups);
        if (StringUtils.hasText(dataBase)) {
            queryWrapper.eq("data_base", dataBase);
        }
        if (StringUtils.hasText(inOrOut)) {
            queryWrapper.eq("in_or_out", inOrOut);
        }

        List<GisManageTemplate> templates = templateMapper.selectList(queryWrapper);
        log.info("根据分组 {} 查询到 {} 个模板", groups, templates.size());
        return templates;
    }

    @Override
    public List<GisManageTemplate> getTemplatesByType(String templateType) {
        if (!StringUtils.hasText(templateType)) {
            throw new IllegalArgumentException("模板类型不能为空");
        }

        QueryWrapper<GisManageTemplate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("template_type", templateType);
        List<GisManageTemplate> templates = templateMapper.selectList(queryWrapper);
        log.info("根据模板类型 {} 查询到 {} 个模板", templateType, templates.size());
        return templates;
    }

    @Override
    public List<GisManageTemplate> getTemplatesByGeometryType(Integer geometryType) {
        if (geometryType == null) {
            throw new IllegalArgumentException("几何类型不能为空");
        }

        List<GisManageTemplate> templates = templateMapper.selectByGeometryType(geometryType);
        log.info("根据几何类型 {} 查询到 {} 个模板", geometryType, templates.size());
        return templates;
    }

    @Override
    public List<GisManageTemplate> getAllImportTemplates() {
        List<GisManageTemplate> templates = templateMapper.selectAllImportTemplates();
        log.info("查询到 {} 个导入模板", templates.size());
        return templates;
    }

    @Override
    public GisManageTemplate getTemplateBySheetNameAndGroups(String sheetName, String groups, String dataBase) {
        if (!StringUtils.hasText(sheetName) || !StringUtils.hasText(groups)) {
            throw new IllegalArgumentException("Sheet名称和分组名称不能为空");
        }

        GisManageTemplate template = templateMapper.selectBySheetNameAndGroups(sheetName, groups, dataBase);
        if (template != null) {
            log.info("根据Sheet名称 {} 和分组 {} 查询到模板: {}", sheetName, groups, template.getNameZh());
        } else {
            log.warn("根据Sheet名称 {} 和分组 {} 未查询到模板", sheetName, groups);
        }
        return template;
    }

    @Override
    public Integer createTemplate(GisManageTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("模板信息不能为空");
        }

        // 设置创建时间
        template.setCreateTime(new Date());

        // 设置默认值
        if (template.getInOrOut() == null) {
            template.setInOrOut("in");
        }
        if (template.getTemplateType() == null) {
            template.setTemplateType("shp");
        }

        int result = templateMapper.insert(template);
        if (result > 0) {
            log.info("创建模板成功，模板ID: {}, 模板名称: {}", template.getId(), template.getNameZh());
            return template.getId();
        } else {
            throw new RuntimeException("创建模板失败");
        }
    }

    @Override
    public Boolean updateTemplate(GisManageTemplate template) {
        if (template == null || template.getId() == null) {
            throw new IllegalArgumentException("模板信息或模板ID不能为空");
        }

        // 验证模板是否存在
        GisManageTemplate existingTemplate = templateMapper.selectById(template.getId());
        if (existingTemplate == null) {
            throw new RuntimeException("要更新的模板不存在");
        }

        int result = templateMapper.updateById(template);
        if (result > 0) {
            log.info("更新模板成功，模板ID: {}", template.getId());
            return true;
        } else {
            log.error("更新模板失败，模板ID: {}", template.getId());
            return false;
        }
    }

    @Override
    public Boolean deleteTemplate(Integer templateId) {
        if (templateId == null) {
            throw new IllegalArgumentException("模板ID不能为空");
        }

        // 验证模板是否存在
        GisManageTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("要删除的模板不存在");
        }

        int result = templateMapper.deleteById(templateId);
        if (result > 0) {
            log.info("删除模板成功，模板ID: {}", templateId);
            return true;
        } else {
            log.error("删除模板失败，模板ID: {}", templateId);
            return false;
        }
    }

    @Override
    public Page<GisManageTemplate> getTemplatesPage(Integer id, String tableName, String nameZh, String dataBase, Long pageSize, Long pageIndex) {
        Page<GisManageTemplate> page = new Page<>(pageIndex, pageSize);
        QueryWrapper<GisManageTemplate> queryWrapper = new QueryWrapper<>();

        if (id != null) {
            queryWrapper.eq("id", id);
        }
        if (StringUtils.hasText(tableName)) {
            queryWrapper.like("table_name", tableName);
        }
        if (StringUtils.hasText(nameZh)) {
            queryWrapper.like("name_zh", nameZh);
        }
        if (StringUtils.hasText(dataBase)) {
            queryWrapper.eq("data_base", dataBase);
        }

        queryWrapper.orderByAsc("create_time");
        return templateMapper.selectPage(page, queryWrapper);
    }

    @Override
    public List<GisManageTemplate> getTemplatesByDatabase(String database) {
        if (!StringUtils.hasText(database)) {
            throw new IllegalArgumentException("数据库名称不能为空");
        }

        QueryWrapper<GisManageTemplate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("data_base", database);
        return templateMapper.selectList(queryWrapper);
    }

    @Override
    public List<GisManageTemplate> getTemplatesByAppIdAndGroups(String appId, String groups, String templateType, String inOrOut) {
        QueryWrapper<GisManageTemplate> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(appId)) {
            queryWrapper.eq("app_id", appId);
        }
        if (StringUtils.hasText(groups)) {
            queryWrapper.eq("groups", groups);
        }
        if (StringUtils.hasText(templateType)) {
            queryWrapper.eq("template_type", templateType);
        }

        if (!StringUtils.hasText(inOrOut)) {
            inOrOut = "in";
        }
        queryWrapper.eq("in_or_out", inOrOut);

        List<GisManageTemplate> templates = templateMapper.selectList(queryWrapper);
        log.info("查询到 {} 个模板", templates.size());
        return templates;
    }
}
