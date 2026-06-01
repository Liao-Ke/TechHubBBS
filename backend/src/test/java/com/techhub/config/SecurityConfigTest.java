package com.techhub.config;

import com.techhub.security.TestJwtConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
@Sql(scripts = "classpath:sql/h2-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = {"classpath:sql/h2-integration-data.sql", "classpath:sql/h2-security-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
@DisplayName("SecurityConfig 安全配置测试")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/users/me without token → 401")
    void a_getUsersMeWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    @DisplayName("GET /api/v1/users/1 without token → 200 (public profile)")
    void b_getUserByIdWithoutTokenShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/users/1/posts without token → 200")
    void c_getUserPostsWithoutTokenShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/posts"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/recommendations without token → 401")
    void d_getRecommendationsWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/notifications without token → 401")
    void e_getNotificationsWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("OPTIONS /api/v1/posts preflight → NOT 401")
    void f_optionsPreflightShouldNotReturn401() throws Exception {
        mockMvc.perform(options("/api/v1/posts"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
    }

    @Test
    @DisplayName("GET /api/v1/admin/posts as USER role → 403 (access denied)")
    void g_adminEndpointWithUserRoleShouldReturn403() throws Exception {
        // Register testuser via API (uses correct BCryptPasswordEncoder, gets USER role)
        String regJson = "{\"username\":\"testuser\",\"password\":\"password123\",\"email\":\"testuser@test.com\"}";
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(regJson));

        // Login to get a JWT token
        String loginJson = "{\"username\":\"testuser\",\"password\":\"password123\"}";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(loginJson))
                .andReturn().getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String token = mapper.readTree(response).get("data").get("token").asText();

        // Access admin endpoint — triggers accessDeniedHandler since USER lacks MODERATOR/ADMIN role
        mockMvc.perform(get("/api/v1/admin/posts")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
