package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.example.demo",
        "com.gxj.cropyield.common",
        "com.gxj.cropyield.config",
        "com.gxj.cropyield.modules.auth",
        "com.gxj.cropyield.modules.system"
})
@EntityScan(basePackages = {
        "com.example.demo",
        "com.gxj.cropyield.modules.auth",
        "com.gxj.cropyield.modules.system"
})
@EnableJpaRepositories(basePackages = {
        "com.example.demo",
        "com.gxj.cropyield.modules.auth",
        "com.gxj.cropyield.modules.system"
})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
