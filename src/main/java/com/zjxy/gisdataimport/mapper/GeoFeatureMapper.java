package com.zjxy.gisdataimport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjxy.gisdataimport.entity.GeoFeatureEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GeoFeatureMapper extends BaseMapper<GeoFeatureEntity> {
    // 可以在这里添加自定义的查询方法
}
