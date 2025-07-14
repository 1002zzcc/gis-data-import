-- GIS管理模板表
CREATE TABLE IF NOT EXISTS `gis_manage_template` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `table_id` int(11) DEFAULT NULL COMMENT '关联的数据库表ID',
  `table_name` varchar(255) NOT NULL COMMENT '目标数据库表名',
  `datasource_name` varchar(255) DEFAULT 'master' COMMENT '数据源名称',
  `name_zh` varchar(255) NOT NULL COMMENT '模板中文名称',
  `name_en` varchar(255) DEFAULT NULL COMMENT '模板英文名称',
  `th_line` int(11) DEFAULT NULL COMMENT '表数据开始行号（Excel导入时使用）',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `line_type` int(11) DEFAULT NULL COMMENT '线坐标来源：1来源于数据库，2来源于excel表',
  `is_zh` tinyint(1) DEFAULT 0 COMMENT '是否进行坐标转换',
  `type` int(11) DEFAULT NULL COMMENT '模板类型：1纯文本，2点表，3线表',
  `original_coordinate_system` varchar(100) DEFAULT NULL COMMENT '源坐标系',
  `target_coordinate_system` varchar(100) DEFAULT NULL COMMENT '目标坐标系',
  `line_map` json DEFAULT NULL COMMENT '线要素映射配置（JSON格式）',
  `point_map` json DEFAULT NULL COMMENT '点要素映射配置（JSON格式）',
  `map` json DEFAULT NULL COMMENT '字段映射配置（JSON格式）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `template_type` varchar(50) DEFAULT 'shp' COMMENT '模板类型：excel, shp等',
  `data_base` varchar(255) DEFAULT NULL COMMENT '数据库名称',
  `data_base_mode` varchar(100) DEFAULT NULL COMMENT '数据库模式',
  `data_base_table` varchar(255) DEFAULT NULL COMMENT '数据库表名',
  `tsfl` tinyint(1) DEFAULT 0 COMMENT '是否图示分类',
  `txdb` tinyint(1) DEFAULT 0 COMMENT '是否图形多表',
  `uid` varchar(100) DEFAULT NULL COMMENT '用户ID',
  `app_id` varchar(100) DEFAULT NULL COMMENT '应用ID',
  `groups` varchar(255) DEFAULT NULL COMMENT '分组名称',
  `sheet_name` varchar(255) DEFAULT NULL COMMENT 'Sheet名称（Excel导入时使用）',
  `check_rule` tinyint(1) DEFAULT 0 COMMENT '是否启用校验规则',
  `check_rule_id` int(11) DEFAULT NULL COMMENT '校验规则ID',
  `in_or_out` varchar(10) DEFAULT 'in' COMMENT '导入导出标识：in导入，out导出',
  `value_map` json DEFAULT NULL COMMENT '值域映射配置（JSON格式）',
  `layer_en` varchar(255) DEFAULT NULL COMMENT '图层英文名称',
  `association` json DEFAULT NULL COMMENT '关联配置（JSON格式）',
  PRIMARY KEY (`id`),
  KEY `idx_table_name` (`table_name`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_in_or_out` (`in_or_out`),
  KEY `idx_groups` (`groups`),
  KEY `idx_data_base` (`data_base`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GIS管理模板表';

-- 插入示例模板数据
INSERT INTO `gis_manage_template` (
  `table_name`, `name_zh`, `name_en`, `type`, `is_zh`, 
  `original_coordinate_system`, `target_coordinate_system`, 
  `template_type`, `data_base`, `in_or_out`, `map`
) VALUES 
(
  'geo_features', 
  '通用点要素模板', 
  'General Point Feature Template', 
  2, 
  1, 
  'CGCS2000', 
  'CGCS2000XY', 
  'shp', 
  'gisdb', 
  'in',
  JSON_ARRAY(
    JSON_OBJECT(
      'shpFieldName', 'NAME',
      'fieldName', 'feature_name',
      'dataType', 'String',
      'required', true,
      'description', '要素名称'
    ),
    JSON_OBJECT(
      'shpFieldName', 'TYPE',
      'fieldName', 'feature_type',
      'dataType', 'String',
      'required', false,
      'description', '要素类型'
    ),
    JSON_OBJECT(
      'shpFieldName', 'the_geom',
      'fieldName', 'geometry',
      'dataType', 'Geometry',
      'required', true,
      'coordinateTransform', true,
      'description', '几何数据'
    )
  )
),
(
  'geo_features', 
  '通用线要素模板', 
  'General Line Feature Template', 
  3, 
  1, 
  'CGCS2000', 
  'CGCS2000XY', 
  'shp', 
  'gisdb', 
  'in',
  JSON_ARRAY(
    JSON_OBJECT(
      'shpFieldName', 'ROAD_NAME',
      'fieldName', 'road_name',
      'dataType', 'String',
      'required', true,
      'description', '道路名称'
    ),
    JSON_OBJECT(
      'shpFieldName', 'ROAD_TYPE',
      'fieldName', 'road_type',
      'dataType', 'String',
      'required', false,
      'description', '道路类型'
    ),
    JSON_OBJECT(
      'shpFieldName', 'the_geom',
      'fieldName', 'geometry',
      'dataType', 'Geometry',
      'required', true,
      'coordinateTransform', true,
      'description', '几何数据'
    )
  )
);

-- 创建模板验证规则表（可选）
CREATE TABLE IF NOT EXISTS `gis_manage_template_valid` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` int(11) NOT NULL COMMENT '模板ID',
  `field_name` varchar(255) NOT NULL COMMENT '字段名称',
  `validation_type` varchar(50) NOT NULL COMMENT '验证类型：required, datatype, range, regex等',
  `validation_rule` json DEFAULT NULL COMMENT '验证规则配置',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误提示信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_field_name` (`field_name`),
  FOREIGN KEY (`template_id`) REFERENCES `gis_manage_template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GIS管理模板验证规则表';

-- 插入示例验证规则
INSERT INTO `gis_manage_template_valid` (
  `template_id`, `field_name`, `validation_type`, `validation_rule`, `error_message`
) VALUES 
(1, 'feature_name', 'required', JSON_OBJECT('required', true), '要素名称不能为空'),
(1, 'feature_name', 'length', JSON_OBJECT('minLength', 1, 'maxLength', 100), '要素名称长度必须在1-100字符之间'),
(2, 'road_name', 'required', JSON_OBJECT('required', true), '道路名称不能为空'),
(2, 'road_type', 'enum', JSON_OBJECT('values', JSON_ARRAY('高速公路', '国道', '省道', '县道', '乡道')), '道路类型必须是预定义值之一');
