package com.centerton.centerton.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http
                // WebConfig의 CORS 설정을 Spring Security에서도 사용
                .cors(Customizer.withDefaults())

                // REST API Postman 테스트를 위해 임시 비활성화
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // 화상상담 API는 개발 테스트 중 임시 허용
                        .requestMatchers(
                                "/api/consultations/**"
                        ).permitAll()

                        // 정적 업로드 파일 접근 허용
                        .requestMatchers(
                                "/uploads/**"
                        ).permitAll()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                .build();
    }
}