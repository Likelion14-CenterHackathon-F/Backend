package com.centerton.centerton.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
public class UtcTimeConfig {

    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    public DateTimeProvider utcDateTimeProvider(Clock utcClock) {
        return () -> Optional.of(LocalDateTime.now(utcClock));
    }
}
