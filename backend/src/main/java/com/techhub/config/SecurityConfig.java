package com.techhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 * 无状态 JWT 认证，BCrypt 密码加密，方法级安全控制
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 认证接口开放
                .requestMatchers("/api/v1/auth/**").permitAll()
                // GET 读取类接口开放
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/posts/**",
                    "/api/v1/categories/**",
                    "/api/v1/notices/**",
                    "/api/v1/users/*"
                ).permitAll()
                // Swagger / Knife4j 文档
                .requestMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 管理后台接口需 ADMIN 或 MODERATOR 角色
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "MODERATOR")
                // 其余接口需认证
                .anyRequest().authenticated()
            )
            // 无状态 REST API，禁用 CSRF
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
