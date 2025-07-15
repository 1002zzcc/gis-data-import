-- 检查模板ID 1830的详细信息
SELECT 
    id,
    table_name,
    name_zh,
    name_en,
    original_coordinate_system,
    target_coordinate_system,
    is_zh,
    type,
    template_type,
    data_base,
    data_base_mode,
    data_base_table,
    in_or_out,
    map,
    line_map,
    point_map,
    create_time
FROM gis_manage_template 
WHERE id = 1830;

-- 检查是否存在模板ID 1830
SELECT COUNT(*) as template_exists FROM gis_manage_template WHERE id = 1830;

-- 查看所有可用的模板
SELECT 
    id,
    table_name,
    name_zh,
    template_type,
    in_or_out,
    CASE 
        WHEN table_name IS NULL OR table_name = '' THEN 'EMPTY'
        WHEN table_name = 'geo_features' THEN 'DEFAULT'
        ELSE 'CUSTOM'
    END as table_type
FROM gis_manage_template 
WHERE in_or_out = 'in' 
ORDER BY id;

-- 检查模板1830的字段映射JSON
SELECT 
    id,
    table_name,
    name_zh,
    LENGTH(map) as map_json_length,
    map as map_json_content
FROM gis_manage_template 
WHERE id = 1830;

-- 如果模板1830不存在，查看最近的几个模板
SELECT 
    id,
    table_name,
    name_zh,
    original_coordinate_system,
    target_coordinate_system,
    is_zh
FROM gis_manage_template 
WHERE in_or_out = 'in' 
ORDER BY id DESC 
LIMIT 5;
