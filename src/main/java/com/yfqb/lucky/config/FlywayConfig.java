package com.yfqb.lucky.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 数据库迁移配置
 * <p>
 * 项目使用 R2DBC 而非 JDBC，Spring Boot 无法自动为 Flyway 创建数据源，
 * 因此手动配置 Flyway，直接使用配置中的 JDBC URL 连接数据库执行迁移。
 */
@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(FlywayProperties properties) {
        return Flyway.configure()
                .dataSource(
                        properties.getUrl(),
                        properties.getUser(),
                        properties.getPassword()
                )
                .locations(properties.getLocations().toArray(new String[0]))
                .baselineOnMigrate(properties.isBaselineOnMigrate())
                .createSchemas(properties.isCreateSchemas())
                .load();
    }
}
