package com.zjxy.gisdataimport.config.dynamic;

import com.alibaba.druid.pool.DruidDataSource;
import com.zjxy.gisdataimport.entity.GisManageDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Map;

/**
 * 数据源工具类
 * 用于动态创建和管理数据源
 */
@Component
@Slf4j
public class DataSourceUtils {

    @Autowired
    private DynamicDataSource dynamicDataSource;

    /**
     * &#064;Description:  根据传递的数据源信息测试数据库连接
     * &#064;Author   zzc
     */
    public DruidDataSource createDataSourceConnection(GisManageDataSource dataSourceInfo) {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl(dataSourceInfo.getUrl());
        druidDataSource.setUsername(dataSourceInfo.getUsername());
        druidDataSource.setPassword(dataSourceInfo.getPassword());
        druidDataSource.setDriverClassName(dataSourceInfo.getDriver());
        druidDataSource.setBreakAfterAcquireFailure(true);
        druidDataSource.setConnectionErrorRetryAttempts(0);
        try {
            druidDataSource.getConnection(2000);
            log.info("数据源连接成功");
            return druidDataSource;
        } catch (SQLException throwables) {
            log.error("数据源 {} 连接失败,用户名：{}，密码 {}", dataSourceInfo.getUrl(), dataSourceInfo.getUsername(), dataSourceInfo.getPassword());
            return null;
        }
    }

    /**
     * @Description: 将新增的数据源加入到备份数据源map中
     * @Author zzc
     */
    public void addDefineDynamicDataSource(DruidDataSource druidDataSource, String dataSourceName) {
        Map<Object, Object> defineTargetDataSources = dynamicDataSource.getDefineTargetDataSources();
        if(defineTargetDataSources.get(dataSourceName) == null) {
            defineTargetDataSources.put(dataSourceName, druidDataSource);
            dynamicDataSource.setTargetDataSources(defineTargetDataSources);
        }
        dynamicDataSource.afterPropertiesSet();
    }

    /**
     * 检查数据源是否存在
     */
    public Boolean hasDataSource(String dataSourceName) {
        Map<Object, Object> defineTargetDataSources = dynamicDataSource.getDefineTargetDataSources();
        if(defineTargetDataSources.get(dataSourceName) == null) {
            return false;
        }else{
            return true;
        }
    }

    /**
     * 测试数据源连接
     */
    public Boolean getConnection(String dataSourceName) {
        Map<Object, Object> defineTargetDataSources = dynamicDataSource.getDefineTargetDataSources();
        DruidDataSource druidDataSource = (DruidDataSource)defineTargetDataSources.get(dataSourceName);
        try {
            druidDataSource.getConnection(2000);
            log.info("数据源连接成功");
            return true;
        } catch (SQLException throwables) {
            log.error("数据源连接失败", throwables);
            return false;
        }
    }
}
