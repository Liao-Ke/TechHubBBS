package com.techhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.security.JwtTokenProvider;
import com.techhub.security.TestJwtConfig;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test base class.
 * <p>
 * Subclasses should use {@code @Sql} to set up H2 schema and test data.
 * Real Spring Security filters are active — JWT tokens flow through the full auth chain.
 * Only {@code AiClient} is mocked (external service).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    /**
     * Generate a real JWT for a user with given id, username, and role.
     */
    protected String generateToken(Long userId, String username, String role) {
        return jwtTokenProvider.generateToken(userId, username, role);
    }

    /**
     * Generate a Bearer token header value.
     */
    protected String bearerToken(String token) {
        return "Bearer " + token;
    }

    /**
     * Assert that a ResultActions returns code 200 and the given data field is present.
     */
    protected ResultActions assertOk(ResultActions resultActions) throws Exception {
        return resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    /**
     * Assert unauthorized (401).
     */
    protected ResultActions assertUnauthorized(ResultActions resultActions) throws Exception {
        return resultActions.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
    }

    /**
     * Assert forbidden (403).
     */
    protected ResultActions assertForbidden(ResultActions resultActions) throws Exception {
        return resultActions.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    /**
     * Assert not found (404).
     */
    protected ResultActions assertNotFound(ResultActions resultActions) throws Exception {
        return resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404));
    }

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
