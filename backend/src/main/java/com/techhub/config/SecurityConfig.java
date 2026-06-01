package com.techhub.config;

import com.techhub.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 * 无状态 JWT 认证，BCrypt 密码加密，方法级安全控制
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if ("OPTIONS".equals(request.getMethod())) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        return;
                    }
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write("{\"code\":403,\"message\":\"没有访问权限\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints — public
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Public read endpoints
                .requestMatchers("GET", "/api/v1/posts/**").permitAll()
                .requestMatchers("GET", "/api/v1/categories/**").permitAll()
                .requestMatchers("GET", "/api/v1/notices/**").permitAll()
                .requestMatchers("GET", "/api/v1/users/{id:[0-9]+}").permitAll()
                .requestMatchers("GET", "/api/v1/users/{id:[0-9]+}/posts").permitAll()
                // Swagger / Knife4j — public
                .requestMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                // File access — public
                .requestMatchers("/file/**").permitAll()
                // Admin — restricted
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "MODERATOR")
                // Everything else — authenticated
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
