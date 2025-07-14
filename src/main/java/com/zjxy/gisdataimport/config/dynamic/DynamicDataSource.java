package com.zjxy.gisdataimport.config.dynamic;

/**
 * @Description: 动态数据源
 * @Author: yyalin
 * @CreateDate: 2023/7/16 14:46
 * @Version: V1.0
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DynamicDataSource extends AbstractRoutingDataSource {
    //备份所有数据源信息
    private Map<Object, Object> defineTargetDataSources;

    /**
     * 决定当前线程使用哪个数据源
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceHolder.getDynamicDataSourceKey();
    }
}
