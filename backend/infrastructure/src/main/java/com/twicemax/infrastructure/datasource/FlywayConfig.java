package com.twicemax.infrastructure.datasource;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 多数据源下的 Flyway 配置。
 * <p>
 * 项目同时连三个库（twicemax_user / twicemax_zh / twicemax_en），每个库独立记账
 * （各自的 flyway_schema_history 表）。
 * <p>
 * 脚本目录：
 * <ul>
 *   <li>classpath:db/migration/user     → twicemax_user</li>
 *   <li>classpath:db/migration/business → twicemax_zh 和 twicemax_en 共用一套</li>
 * </ul>
 * <p>
 * baselineOnMigrate = true：首次启动时，如果目标库没有 flyway_schema_history，
 * 自动以 baselineVersion = 1 作为起点。不会重跑历史（比如原来的 001_*.sql），
 * 从 V2 开始才真正执行。
 * <p>
 * 注意：application.yml 里 spring.flyway.enabled = false，禁掉 Spring Boot 的
 * 自动配置，全靠本类的 bean 驱动。三个 bean 的 initMethod = "migrate"，Spring 启动
 * 时会自动调用，应用容器起来前 schema 已经到位。
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flywayUser(@Qualifier("userDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/user")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .baselineDescription("Existing schema before Flyway was introduced")
                .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway flywayBusinessZh(@Qualifier("businessZhDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/business")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .baselineDescription("Existing schema before Flyway was introduced")
                .load();
    }

    @Bean(initMethod = "migrate")
    public Flyway flywayBusinessEn(@Qualifier("businessEnDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/business")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .baselineDescription("Existing schema before Flyway was introduced")
                .load();
    }
}
