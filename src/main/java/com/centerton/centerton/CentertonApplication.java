package com.centerton.centerton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class CentertonApplication {

    public static void main(String[] args) {
        SpringApplication.run(CentertonApplication.class, args);
    }

}
