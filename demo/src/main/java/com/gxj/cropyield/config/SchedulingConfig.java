package com.gxj.cropyield.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * 全局配置模块的配置类，开启基于注解的定时任务支持。
 */

@Configuration
@EnableScheduling
public class SchedulingConfig {
}
