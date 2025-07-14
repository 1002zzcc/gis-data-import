package com.zjxy.gisdataimport.dto;

import lombok.Data;

/**
 * 系统数据库DTO
 */
@Data
public class SysDatabaseDTO {
    
    private Integer id;
    
    /**
     * 数据库中文名称
     */
    private String nameZh;
    
    /**
     * 数据库英文名称
     */
    private String nameEn;
    
    /**
     * 数据库IP地址
     */
    private String ip;
    
    /**
     * 数据库端口
     */
    private String port;
    
    /**
     * 数据库用户名
     */
    private String username;
    
    /**
     * 数据库密码（Base64编码）
     */
    private String password;
    
    /**
     * 数据库类型
     */
    private String type;
    
    /**
     * 数据库驱动
     */
    private String driver;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 描述
     */
    private String description;
}
