package com.gxj.cropyield;

import com.gxj.cropyield.common.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
    basePackages = "com.gxj.cropyield",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.gxj\\.cropyield\\.config\\.SecurityConfig"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.gxj\\.cropyield\\.common\\.config\\.SecurityConfig")
    }
)
@EnableConfigurationProperties(JwtProperties.class)
public class CropYieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(CropYieldApplication.class, args);
    }

}
