package com.gxj.cropyield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.gxj.cropyield")
@EnableJpaRepositories(basePackages = "com.gxj.cropyield")
public class CropYieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(CropYieldApplication.class, args);
    }

}
