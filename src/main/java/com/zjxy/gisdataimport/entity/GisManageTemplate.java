package com.zjxy.gisdataimport.entity;

public class GisManageTemplate {
    private Integer id;

    private Integer tableId;

    private String tableName;

    private String datasourceName;

    private String nameZh;

    private String nameEn;
    /**
     * (模板类型，1纯文本，2点表， 3线表)
     */
    private Integer type;

    private String originalCoordinateSystem;

    private String targetCoordinateSystem;
}
