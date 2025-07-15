package com.zjxy.gisdataimport.mapper;


import com.zjxy.gisdataimport.dto.SysDatabaseDTO;

public interface DataSourceMapper {
    SysDatabaseDTO selectById(String databaseId);

    SysDatabaseDTO selectByDatabaseName(String databaseName);
}
