package com.techhub.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenProvider 单元测试")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private SecretKey secretKey;
    private long expirationMs;

    @BeforeEach
    void setUp() {
        secretKey = Jwts.SIG.HS256.key().build();
        expirationMs = 3600000L;
        tokenProvider = new JwtTokenProvider(secretKey, expirationMs);
    }

    @Test
    @DisplayName("generateToken — 返回非空字符串")
    void testGenerateToken_ReturnsNonEmptyString() {
        String token = tokenProvider.generateToken(1L, "admin", "ADMIN");

        assertNotNull(token, "生成的 Token 不应为 null");
        assertFalse(token.isEmpty(), "生成的 Token 不应为空字符串");
        String jwtPattern = "^[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+$";
        assertTrue(token.matches(jwtPattern), "Token 格式应为三段式 JWT");
    }

    @Test
    @DisplayName("getUserIdFromToken — 正确提取用户 ID")
    void testGetUserIdFromToken_CorrectlyExtracted() {
        long expectedUserId = 123L;
        String token = tokenProvider.generateToken(expectedUserId, "testuser", "USER");

        Long extractedUserId = tokenProvider.getUserIdFromToken(token);

        assertEquals(expectedUserId, extractedUserId, "提取的 userId 应等于原始值");
    }

    @Test
    @DisplayName("validateToken — 合法 Token 返回 true")
    void testValidateToken_ValidToken_ReturnsTrue() {
        String token = tokenProvider.generateToken(1L, "user", "USER");

        boolean isValid = tokenProvider.validateToken(token);

        assertTrue(isValid, "合法 Token 应验证通过");
    }

    @Test
    @DisplayName("validateToken — 过期 Token 返回 false")
    void testValidateToken_ExpiredToken_ReturnsFalse() throws InterruptedException {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secretKey, 1L);
        String token = shortLivedProvider.generateToken(1L, "user", "USER");

        Thread.sleep(10);

        boolean isValid = shortLivedProvider.validateToken(token);

        assertFalse(isValid, "过期 Token 应验证失败");
    }

    @Test
    @DisplayName("validateToken — 被篡改 Token 返回 false")
    void testValidateToken_TamperedToken_ReturnsFalse() {
        String token = tokenProvider.generateToken(1L, "user", "USER");
        String tamperedToken = token + "x";

        boolean isValid = tokenProvider.validateToken(tamperedToken);

        assertFalse(isValid, "被篡改 Token 应验证失败");
    }
}
