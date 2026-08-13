package com.centerton.centerton.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final String[] allowedOriginPatterns;

    public WebConfig(
            @Value("${cors.allowed-origins:https://kanage.vercel.app,http://localhost:5173}") String allowedOrigins,
            @Value("${cors.allowed-origin-patterns:}") String allowedOriginPatterns
    ) {
        this.allowedOrigins = parseCsv(allowedOrigins);
        this.allowedOriginPatterns = parseCsv(allowedOriginPatterns);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsRegistration registration = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        if (allowedOrigins.length > 0) {
            registration.allowedOrigins(allowedOrigins);
        }
        if (allowedOriginPatterns.length > 0) {
            registration.allowedOriginPatterns(allowedOriginPatterns);
        }
    }

    private String[] parseCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }
}
