package com.zjxy.gisdataimport.service.Impl;

import com.alibaba.druid.pool.DruidDataSource;
import com.zjxy.gisdataimport.config.dynamic.DataSourceUtils;
import com.zjxy.gisdataimport.config.dynamic.DynamicDataSourceHolder;
import com.zjxy.gisdataimport.dto.SysDatabaseDTO;
import com.zjxy.gisdataimport.entity.GisManageDataSource;
import com.zjxy.gisdataimport.mapper.DataSourceMapper;
import com.zjxy.gisdataimport.service.DataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



/**
 * 数据源服务实现类
 */
@Service
@Slf4j
public class DataSourceServiceImpl implements DataSourceService {
    DataSourceMapper dataSourceMapper;

    @Autowired
    private DataSourceUtils dataSourceUtils;

    @Override
    public void changeDatabase(SysDatabaseDTO sysDatabaseDTO) {
        Boolean aBoolean = dataSourceUtils.hasDataSource(sysDatabaseDTO.getNameEn());
        if(aBoolean){
            if (!DynamicDataSourceHolder.getDynamicDataSourceKey().equals(sysDatabaseDTO.getNameEn())) {
                DynamicDataSourceHolder.setDynamicDataSourceKey(sysDatabaseDTO.getNameEn());
            }
        }else{
            GisManageDataSource gisManageDataSource = new GisManageDataSource();
            gisManageDataSource.setDriver("org.postgresql.Driver");
            // 直接使用byte[]密码
            gisManageDataSource.setPassword(new String(sysDatabaseDTO.getPassword()));
            gisManageDataSource.setUsername(sysDatabaseDTO.getUsername());
            gisManageDataSource.setUrl(getPostgreSqlUrl(sysDatabaseDTO));
            DruidDataSource dataSourceConnection = dataSourceUtils.createDataSourceConnection(gisManageDataSource);
            if(dataSourceConnection != null){
                dataSourceUtils.addDefineDynamicDataSource(dataSourceConnection, sysDatabaseDTO.getNameEn());
                //设置当前线程数据源名称
                DynamicDataSourceHolder.setDynamicDataSourceKey(sysDatabaseDTO.getNameEn());
            }
        }
    }

    @Override
    public void clearDataSource(){
        DynamicDataSourceHolder.removeDynamicDataSourceKey();
    }

    @Override
    public SysDatabaseDTO selectByDatabaseName(String databaseName) {
        SysDatabaseDTO sysDatabaseDTO = dataSourceMapper.selectByDatabaseName(databaseName);
        return sysDatabaseDTO;
    }

    @Override
    public Boolean testConnection(SysDatabaseDTO sysDatabaseDTO) {
        try {
            GisManageDataSource gisManageDataSource = new GisManageDataSource();
            gisManageDataSource.setDriver("org.postgresql.Driver");
            gisManageDataSource.setPassword(new String(sysDatabaseDTO.getPassword()));
            gisManageDataSource.setUsername(sysDatabaseDTO.getUsername());
            gisManageDataSource.setUrl(getPostgreSqlUrl(sysDatabaseDTO));

            DruidDataSource dataSourceConnection = dataSourceUtils.createDataSourceConnection(gisManageDataSource);
            return dataSourceConnection != null;
        } catch (Exception e) {
            log.error("测试数据库连接失败", e);
            return false;
        }
    }

    /**
     * 构建PostgreSQL连接URL
     */
    private String getPostgreSqlUrl(SysDatabaseDTO sysDatabase){
        return "jdbc:postgresql://" + sysDatabase.getIp() + ":" + sysDatabase.getPort() + "/" + sysDatabase.getNameEn();
    }
}
