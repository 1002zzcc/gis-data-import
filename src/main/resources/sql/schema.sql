-- 创建GIS数据导入数据库表
CREATE DATABASE IF NOT EXISTS gisdb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE gisdb;

-- 地理要素表
drop  table if exists geo_features;
CREATE TABLE IF NOT EXISTS geo_features (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    feature_id VARCHAR(255) NOT NULL COMMENT '要素ID',
    geometry LONGTEXT COMMENT '几何信息(WKT格式)',
    attributes LONGTEXT COMMENT '属性信息(JSON格式)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_feature_id (feature_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地理要素数据表';

-- 优化表结构用于大数据量插入
-- 设置合适的存储引擎参数
ALTER TABLE geo_features
    ENGINE=InnoDB
    ROW_FORMAT=DYNAMIC;
