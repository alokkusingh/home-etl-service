package com.alok.home.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;


@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OncePerRequestFilter customAuthenticationFilter) throws Exception {
        final String[] EXCLUDED_PATTERNS = {
                "/h2-console/**"
        };
        http.headers(headers -> headers.frameOptions(frameOptionsConfig -> {
            frameOptionsConfig.disable();
            frameOptionsConfig.sameOrigin();
        }));

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator", "/actuator/**").permitAll();
                    auth.requestMatchers("/gsheet/**").hasAnyRole("ADMIN", "USER", "LOCALHOST", "home_api_rw");
                    auth.requestMatchers("/form/**").hasAnyRole("ADMIN", "USER", "LOCALHOST", "home_api_rw");
                    auth.requestMatchers("/search/**").hasAnyRole("ADMIN", "USER", "LOCALHOST", "home_api_rw");

                    auth.requestMatchers("/report", "/report/**").hasAnyRole("ADMIN");
                    auth.requestMatchers("/file", "/file/**").hasAnyRole("ADMIN", "LOCALHOST");
                    auth.requestMatchers("/h2-console", "/h2-console/**").hasAnyRole("ADMIN", "LOCALHOST");

                    auth.anyRequest().authenticated();
                })
                //.httpBasic(Customizer.withDefaults())
                .build();
    }
}
