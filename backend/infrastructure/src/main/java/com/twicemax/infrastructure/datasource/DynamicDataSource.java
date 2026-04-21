package com.twicemax.infrastructure.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源
 * 根据 DataSourceContextHolder 中存储的语言标识选择对应的业务数据源
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        // 返回当前语言作为数据源的 key
        // 这个 key 会用来从 targetDataSources Map 中查找对应的数据源
        return DataSourceContextHolder.getLanguage();
    }
}
