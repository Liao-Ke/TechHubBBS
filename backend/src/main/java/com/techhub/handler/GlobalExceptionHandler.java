package com.techhub.handler;

import com.techhub.common.BusinessException;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getCode()).body(R.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return R.error(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return R.error(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return ResponseEntity.status(ResultCode.FORBIDDEN.getCode())
                .body(R.error(ResultCode.FORBIDDEN, "没有访问权限"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {} {}", e.getMethod(), e.getMessage());
        return R.error(ResultCode.BAD_REQUEST, "不支持的请求方法: " + e.getMethod());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.error(ResultCode.INTERNAL_ERROR);
    }
}
