package com.zjxy.gisdataimport.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统数据库DTO
 */
@Data
public class SysDatabaseDTO implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField(value = "name_en")
    private String nameEn;

    @TableField(value = "name_zh")
    private String nameZh;


    @TableField(value = "ip")
    private String ip;

    @TableField(value = "port")
    private String port;

    @TableField(value = "username")
    private String username;

    @TableField(value = "password")
    private byte[] password;

    @TableField(value = "type")
    private String type;

    @TableField(value = "driver")
    private String driver;

    @TableField(value = "enabled")
    private Boolean enabled;

    @TableField(value = "description")
    private String description;

    @TableField(value = "createdate")
    private Date createDate;
}
