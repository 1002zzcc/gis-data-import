-- 1. 建表
CREATE TABLE IF NOT EXISTS "public"."t_gas_point_cs" (
                                                         "id"  SERIAL PRIMARY KEY,
                                                         "geom" GEOMETRY(POINT, 4326),
                                                         "gjz"  VARCHAR(255),
                                                         "x"    DOUBLE PRECISION,
                                                         "y"    DOUBLE PRECISION
);

-- 2. 空间索引
CREATE INDEX IF NOT EXISTS "idx_t_gas_point_cs_geom"
    ON "public"."t_gas_point_cs" USING GIST ("geom");

-- 3. 表注释
COMMENT ON TABLE "public"."t_gas_point_cs" IS '模板生成: 燃气点位测试表';

-- 4. 字段注释
COMMENT ON COLUMN "public"."t_gas_point_cs"."id"  IS '主键';
COMMENT ON COLUMN "public"."t_gas_point_cs"."geom" IS '空间坐标（WGS84）';
COMMENT ON COLUMN "public"."t_gas_point_cs"."gjz"  IS '关键字';
COMMENT ON COLUMN "public"."t_gas_point_cs"."x"    IS '经度';
COMMENT ON COLUMN "public"."t_gas_point_cs"."y"    IS '纬度';
