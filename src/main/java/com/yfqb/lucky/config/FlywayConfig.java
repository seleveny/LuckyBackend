package com.yfqb.lucky.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 数据库迁移配置
 * <p>
 * 项目使用 R2DBC 而非 JDBC，Spring Boot 无法自动为 Flyway 创建数据源，
 * 因此手动配置 Flyway，从 R2DBC 配置中提取数据库连接信息。
 */
@Configuration
public class FlywayConfig {

    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        // 将 r2dbc:mysql:// 替换为 jdbc:mysql://，复用同一个数据库连接配置
        String jdbcUrl = r2dbcUrl.replace("r2dbc:mysql://", "jdbc:mysql://")
                + "&allowPublicKeyRetrieval=true";
        return Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db")
                .baselineOnMigrate(true)
                .createSchemas(true)
                .load();
    }
}
