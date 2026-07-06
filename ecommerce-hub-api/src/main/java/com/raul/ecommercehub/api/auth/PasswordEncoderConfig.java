package com.raul.ecommercehub.api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder(
            @Value("${security.argon2.salt-length}") int saltLength,
            @Value("${security.argon2.hash-length}") int hashLength,
            @Value("${security.argon2.parallelism}") int parallelism,
            @Value("${security.argon2.memory-kb}") int memoryKb,
            @Value("${security.argon2.iterations}") int iterations) {
        return new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memoryKb, iterations);
    }
}