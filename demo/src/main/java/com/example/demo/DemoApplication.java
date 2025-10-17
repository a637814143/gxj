package com.example.demo;

import com.gxj.cropyield.CropYieldApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.example.demo",
                "com.gxj.cropyield"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.example\\.demo\\.config\\.SecurityConfig"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.gxj\\.cropyield\\.(common|config)\\.SecurityConfig"),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CropYieldApplication.class)
        }
)
@EntityScan(basePackages = {
        "com.example.demo",
        "com.gxj.cropyield"
})
@EnableJpaRepositories(basePackages = {
        "com.example.demo",
        "com.gxj.cropyield"
})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
