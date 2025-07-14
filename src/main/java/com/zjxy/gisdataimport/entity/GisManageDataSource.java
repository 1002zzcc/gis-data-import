package com.zjxy.gisdataimport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * GIS管理数据源实体类
 */
@Data
public class GisManageDataSource {
    @TableId(type = IdType.AUTO)
    private String id;

    private String username;

    private String password;

    private String driver;

    private String url;

    private String name;
}
