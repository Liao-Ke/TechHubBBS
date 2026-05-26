package com.techhub.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 单元测试")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal — 有效 Token 设置 SecurityContext")
    void testDoFilterInternal_ValidToken_SetsSecurityContext() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken("valid.token.here")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid.token.here")).thenReturn(1L);
        when(jwtTokenProvider.getRoleFromToken("valid.token.here")).thenReturn("ADMIN");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "SecurityContext 应包含认证信息");
        assertEquals(1L, auth.getPrincipal(), "Principal 应为 userId=1L");
        assertEquals("ROLE_ADMIN", auth.getAuthorities().iterator().next().getAuthority(),
                "Authority 应为 ROLE_ADMIN");
    }

    @Test
    @DisplayName("doFilterInternal — 无 Authorization Header 继续过滤器链")
    void testDoFilterInternal_NoAuthHeader_ContinuesChain() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "无 Token 时 SecurityContext 应保持未认证状态");
    }

    @Test
    @DisplayName("doFilterInternal — 无效 Token 清空 SecurityContext")
    void testDoFilterInternal_InvalidToken_ClearsContext() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 预先设置一个认证信息，模拟之前有残留
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
        );

        when(jwtTokenProvider.validateToken("invalid.token.here")).thenReturn(false);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "无效 Token 时 SecurityContext 应被清空");
    }
}
