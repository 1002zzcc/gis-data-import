-- 创建默认的 geo_features 表
-- 这是系统的默认表，用于存储通用的地理要素数据

-- 1. 创建表结构
CREATE TABLE IF NOT EXISTS "public"."geo_features" (
    "id" SERIAL PRIMARY KEY,
    "feature_id" VARCHAR(255),
    "geometry" GEOMETRY,
    "attributes" TEXT,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 创建空间索引
CREATE INDEX IF NOT EXISTS "idx_geo_features_geometry"
    ON "public"."geo_features" USING GIST ("geometry");

-- 3. 创建其他索引
CREATE INDEX IF NOT EXISTS "idx_geo_features_feature_id"
    ON "public"."geo_features" ("feature_id");

CREATE INDEX IF NOT EXISTS "idx_geo_features_created_at"
    ON "public"."geo_features" ("created_at");

-- 4. 表注释
COMMENT ON TABLE "public"."geo_features" IS '通用地理要素表 - 系统默认表';

-- 5. 字段注释
COMMENT ON COLUMN "public"."geo_features"."id" IS '主键ID';
COMMENT ON COLUMN "public"."geo_features"."feature_id" IS '要素ID（来自Shapefile）';
COMMENT ON COLUMN "public"."geo_features"."geometry" IS '几何数据（支持所有几何类型）';
COMMENT ON COLUMN "public"."geo_features"."attributes" IS '属性数据（JSON格式）';
COMMENT ON COLUMN "public"."geo_features"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."geo_features"."updated_at" IS '更新时间';

-- 6. 创建更新时间触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_geo_features_updated_at 
    BEFORE UPDATE ON "public"."geo_features" 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 7. 插入示例数据（可选）
-- INSERT INTO "public"."geo_features" ("feature_id", "geometry", "attributes") 
-- VALUES 
-- ('sample_point_1', ST_GeomFromText('POINT(120.123456 30.654321)', 4326), '{"name": "示例点", "type": "测试"}'),
-- ('sample_line_1', ST_GeomFromText('LINESTRING(120.1 30.1, 120.2 30.2)', 4326), '{"name": "示例线", "length": 100}');

-- 查询验证
SELECT 'geo_features表创建成功！' AS message;
SELECT COUNT(*) AS total_records FROM "public"."geo_features";
