package com.raul.ecommercehub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.raul.ecommercehub.shared.domain")
@EnableJpaRepositories(basePackages = "com.raul.ecommercehub.shared.repository")
public class EcommerceHubApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceHubApiApplication.class, args);
    }
}
