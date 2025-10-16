package com.gxj.cropyield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(
        basePackages = "com.gxj.cropyield",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.gxj\\.cropyield\\.yielddata\\..*"
        )
)
@EntityScan(basePackages = "com.gxj.cropyield")
@EnableJpaRepositories(basePackages = "com.gxj.cropyield")
public class CropYieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(CropYieldApplication.class, args);
    }

}
