package com.zjxy.gisdataimport.service;

import com.zjxy.gisdataimport.dto.SysDatabaseDTO;

/**
 * 数据源服务接口
 */
public interface DataSourceService {

    /**
     * 切换数据库
     * @param sysDatabaseDTO 数据库信息
     */
    void changeDatabase(SysDatabaseDTO sysDatabaseDTO);

    /**
     * 清除数据源
     */
    void clearDataSource();

    /**
     * 根据数据库名称查询数据库信息
     * @param databaseName 数据库名称
     * @return 数据库信息
     */
    SysDatabaseDTO selectByDatabaseName(String databaseName);

    /**
     * 测试数据库连接
     * @param sysDatabaseDTO 数据库信息
     * @return 连接是否成功
     */
    Boolean testConnection(SysDatabaseDTO sysDatabaseDTO);
}
