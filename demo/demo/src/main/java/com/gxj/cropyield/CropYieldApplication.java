package com.gxj.cropyield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
/**
 * 系统应用的启动类，负责引导 Spring Boot 服务加载与运行。
 */

@SpringBootApplication
@EntityScan(basePackages = "com.gxj.cropyield")
@EnableJpaRepositories(basePackages = "com.gxj.cropyield")
public class CropYieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(CropYieldApplication.class, args);
    }

}
