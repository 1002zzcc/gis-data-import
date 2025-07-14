-- PostgreSQL数据库建表脚本
-- 适用于gis-data-import项目

-- 创建数据库（如果不存在）
-- CREATE DATABASE resourcemanage WITH ENCODING 'UTF8';

-- 使用数据库
-- \c resourcemanage;

-- 启用PostGIS扩展（如果需要空间数据支持）
-- CREATE EXTENSION IF NOT EXISTS postgis;

-- GIS管理模板表
CREATE TABLE IF NOT EXISTS gis_manage_template (
    id SERIAL PRIMARY KEY,
    table_id INTEGER,
    table_name VARCHAR(255) NOT NULL,
    datasource_name VARCHAR(255) DEFAULT 'master',
    name_zh VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    th_line INTEGER,
    file_path VARCHAR(500),
    line_type INTEGER,
    is_zh BOOLEAN DEFAULT FALSE,
    type INTEGER,
    original_coordinate_system VARCHAR(100),
    target_coordinate_system VARCHAR(100),
    line_map JSONB,
    point_map JSONB,
    map JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    template_type VARCHAR(50) DEFAULT 'shp',
    data_base VARCHAR(255),
    data_base_mode VARCHAR(100),
    data_base_table VARCHAR(255),
    tsfl BOOLEAN DEFAULT FALSE,
    txdb BOOLEAN DEFAULT FALSE,
    uid VARCHAR(100),
    app_id VARCHAR(100),
    groups VARCHAR(255),
    sheet_name VARCHAR(255),
    check_rule BOOLEAN DEFAULT FALSE,
    check_rule_id INTEGER,
    in_or_out VARCHAR(10) DEFAULT 'in',
    value_map JSONB,
    layer_en VARCHAR(255),
    association JSONB
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_gis_template_table_name ON gis_manage_template(table_name);
CREATE INDEX IF NOT EXISTS idx_gis_template_type ON gis_manage_template(template_type);
CREATE INDEX IF NOT EXISTS idx_gis_template_in_or_out ON gis_manage_template(in_or_out);
CREATE INDEX IF NOT EXISTS idx_gis_template_groups ON gis_manage_template(groups);
CREATE INDEX IF NOT EXISTS idx_gis_template_data_base ON gis_manage_template(data_base);
CREATE INDEX IF NOT EXISTS idx_gis_template_create_time ON gis_manage_template(create_time);

-- 插入示例模板数据
INSERT INTO gis_manage_template (
    table_name, name_zh, name_en, type, is_zh, 
    original_coordinate_system, target_coordinate_system, 
    template_type, data_base, in_or_out, map
) VALUES 
(
    'geo_features', 
    '通用点要素模板', 
    'General Point Feature Template', 
    2, 
    TRUE, 
    'CGCS2000', 
    'CGCS2000XY', 
    'shp', 
    'resourcemanage', 
    'in',
    '[
        {
            "shpFieldName": "NAME",
            "fieldName": "feature_name",
            "dataType": "String",
            "required": true,
            "description": "要素名称"
        },
        {
            "shpFieldName": "TYPE",
            "fieldName": "feature_type",
            "dataType": "String",
            "required": false,
            "description": "要素类型"
        },
        {
            "shpFieldName": "the_geom",
            "fieldName": "geometry",
            "dataType": "Geometry",
            "required": true,
            "coordinateTransform": true,
            "description": "几何数据"
        }
    ]'::jsonb
),
(
    'geo_features', 
    '通用线要素模板', 
    'General Line Feature Template', 
    3, 
    TRUE, 
    'CGCS2000', 
    'CGCS2000XY', 
    'shp', 
    'resourcemanage', 
    'in',
    '[
        {
            "shpFieldName": "ROAD_NAME",
            "fieldName": "road_name",
            "dataType": "String",
            "required": true,
            "description": "道路名称"
        },
        {
            "shpFieldName": "ROAD_TYPE",
            "fieldName": "road_type",
            "dataType": "String",
            "required": false,
            "description": "道路类型"
        },
        {
            "shpFieldName": "the_geom",
            "fieldName": "geometry",
            "dataType": "Geometry",
            "required": true,
            "coordinateTransform": true,
            "description": "几何数据"
        }
    ]'::jsonb
);

-- 创建模板验证规则表
CREATE TABLE IF NOT EXISTS gis_manage_template_valid (
    id SERIAL PRIMARY KEY,
    template_id INTEGER NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    validation_type VARCHAR(50) NOT NULL,
    validation_rule JSONB,
    error_message VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES gis_manage_template(id) ON DELETE CASCADE
);

-- 创建验证规则表索引
CREATE INDEX IF NOT EXISTS idx_template_valid_template_id ON gis_manage_template_valid(template_id);
CREATE INDEX IF NOT EXISTS idx_template_valid_field_name ON gis_manage_template_valid(field_name);

-- 插入示例验证规则
INSERT INTO gis_manage_template_valid (
    template_id, field_name, validation_type, validation_rule, error_message
) VALUES 
(1, 'feature_name', 'required', '{"required": true}'::jsonb, '要素名称不能为空'),
(1, 'feature_name', 'length', '{"minLength": 1, "maxLength": 100}'::jsonb, '要素名称长度必须在1-100字符之间'),
(2, 'road_name', 'required', '{"required": true}'::jsonb, '道路名称不能为空'),
(2, 'road_type', 'enum', '{"values": ["高速公路", "国道", "省道", "县道", "乡道"]}'::jsonb, '道路类型必须是预定义值之一');

-- 创建地理要素数据表
CREATE TABLE IF NOT EXISTS geo_features (
    id BIGSERIAL PRIMARY KEY,
    feature_id VARCHAR(255),
    geometry TEXT,  -- 存储WKT格式的几何数据
    attributes JSONB,  -- 存储属性数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建geo_features表索引
CREATE INDEX IF NOT EXISTS idx_geo_features_feature_id ON geo_features(feature_id);
CREATE INDEX IF NOT EXISTS idx_geo_features_created_at ON geo_features(created_at);
CREATE INDEX IF NOT EXISTS idx_geo_features_attributes ON geo_features USING GIN(attributes);

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为geo_features表创建更新时间触发器
CREATE TRIGGER update_geo_features_updated_at 
    BEFORE UPDATE ON geo_features 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- 添加表注释
COMMENT ON TABLE gis_manage_template IS 'GIS管理模板表';
COMMENT ON TABLE gis_manage_template_valid IS 'GIS管理模板验证规则表';
COMMENT ON TABLE geo_features IS '地理要素数据表';

-- 添加字段注释
COMMENT ON COLUMN gis_manage_template.id IS '主键ID';
COMMENT ON COLUMN gis_manage_template.table_name IS '目标数据库表名';
COMMENT ON COLUMN gis_manage_template.name_zh IS '模板中文名称';
COMMENT ON COLUMN gis_manage_template.type IS '模板类型：1纯文本，2点表，3线表';
COMMENT ON COLUMN gis_manage_template.is_zh IS '是否进行坐标转换';
COMMENT ON COLUMN gis_manage_template.map IS '字段映射配置（JSON格式）';

COMMENT ON COLUMN geo_features.id IS '主键ID';
COMMENT ON COLUMN geo_features.feature_id IS '要素ID';
COMMENT ON COLUMN geo_features.geometry IS '几何数据（WKT格式）';
COMMENT ON COLUMN geo_features.attributes IS '属性数据（JSON格式）';

-- 查询验证
SELECT 'PostgreSQL数据库表创建完成' AS status;
SELECT COUNT(*) AS template_count FROM gis_manage_template;
SELECT COUNT(*) AS validation_rule_count FROM gis_manage_template_valid;
