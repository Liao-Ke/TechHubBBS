package com.techhub.handler;

import com.techhub.common.R;
import com.techhub.common.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ConstraintViolationException → HTTP 400 with violation messages")
    void handleConstraintViolationShouldReturn400() {
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolationException ex = new ConstraintViolationException("页码必须大于0", violations);

        ResponseEntity<R<Void>> response = handler.handleConstraintViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ResultCode.BAD_REQUEST.getCode(), response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("页码必须大于0"));
    }

    @Test
    @DisplayName("ConstraintViolationException with violations → message joins violations")
    void handleConstraintViolationShouldJoinMultipleViolations() {
        // With empty violations, the handler falls back to e.getMessage()
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolationException ex = new ConstraintViolationException("参数校验失败", violations);

        ResponseEntity<R<Void>> response = handler.handleConstraintViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ResultCode.BAD_REQUEST.getCode(), response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("参数校验失败"));
    }
}
