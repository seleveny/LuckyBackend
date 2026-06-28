package com.yfqb.lucky.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 * <p>
 * 启用 Spring 的 @Scheduled 注解支持。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
