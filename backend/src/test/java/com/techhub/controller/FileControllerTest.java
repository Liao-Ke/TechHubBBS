package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.security.JwtTokenProvider;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("FileController 单元测试")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    // ==================== POST /api/v1/files/upload ====================

    @Test
    @DisplayName("uploadFile — 上传有效图片返回 200 和文件 URL")
    void uploadFile_ValidImage_Returns200() throws Exception {
        authenticateUser();

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "fake-image-data".getBytes());

        org.dromara.x.file.storage.core.FileInfo fileInfo = new org.dromara.x.file.storage.core.FileInfo();
        fileInfo.setUrl("http://localhost:8080/file/user_avatar/abc123.jpg");
        fileInfo.setFilename("abc123.jpg");
        fileInfo.setSize(14L);

        UploadPretreatment pretreatment = mock(UploadPretreatment.class);
        when(fileStorageService.of(any())).thenReturn(pretreatment);
        when(pretreatment.setPath(anyString())).thenReturn(pretreatment);
        when(pretreatment.upload()).thenReturn(fileInfo);

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("objectType", "user_avatar")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value("http://localhost:8080/file/user_avatar/abc123.jpg"))
                .andExpect(jsonPath("$.data.filename").value("abc123.jpg"))
                .andExpect(jsonPath("$.data.size").value(14));
    }

    @Test
    @DisplayName("uploadFile — 不支持的文件格式返回 400")
    void uploadFile_InvalidFormat_Returns400() throws Exception {
        authenticateUser();

        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "fake-pdf-data".getBytes());

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("objectType", "user_avatar")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("uploadFile — 文件超过 5MB 返回 400")
    void uploadFile_Oversized_Returns400() throws Exception {
        authenticateUser();

        byte[] largeData = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.png", "image/png", largeData);

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("objectType", "post_image")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
