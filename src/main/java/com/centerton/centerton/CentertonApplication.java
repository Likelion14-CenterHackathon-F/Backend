package com.centerton.centerton;

import com.centerton.centerton.global.config.UtcTimeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
@Import(UtcTimeConfig.class)
@SpringBootApplication
public class CentertonApplication {

    public static void main(String[] args) {
        SpringApplication.run(CentertonApplication.class, args);
    }

}
