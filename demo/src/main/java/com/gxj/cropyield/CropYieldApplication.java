package com.gxj.cropyield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.gxj.cropyield.modules",
    "com.gxj.cropyield.common",
    "com.gxj.cropyield.config"
})
@EntityScan(basePackages = "com.gxj.cropyield.modules")
@EnableJpaRepositories(basePackages = "com.gxj.cropyield.modules")
public class CropYieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(CropYieldApplication.class, args);
    }

}
