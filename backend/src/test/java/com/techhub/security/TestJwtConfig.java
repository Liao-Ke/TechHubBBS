package com.techhub.security;

import javax.crypto.SecretKey;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration providing a JwtTokenProvider with hardcoded test values.
 */
@TestConfiguration
public class TestJwtConfig {

    @Bean
    @Primary
    public JwtTokenProvider testJwtTokenProvider() {
        String base64Secret = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi1tdXN0LWJlLWF0LWxlYXN0LTI1Ni1iaXRzLWxvbmctZW5vdWdo";
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        return new JwtTokenProvider(key, 3600000);
    }
}
