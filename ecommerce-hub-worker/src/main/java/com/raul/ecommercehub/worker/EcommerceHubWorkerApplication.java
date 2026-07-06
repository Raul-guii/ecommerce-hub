package com.raul.ecommercehub.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.raul.ecommercehub.shared.domain")
@EnableJpaRepositories(basePackages = "com.raul.ecommercehub.shared.domain")
public class EcommerceHubWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceHubWorkerApplication.class, args);
    }
}
