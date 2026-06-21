package com.jsy.crmeb.modern.admin.config;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.admin.AdminAuthException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class AdminExceptionAdvice {
    private static final Logger logger = LoggerFactory.getLogger(AdminExceptionAdvice.class);

    @ExceptionHandler(AdminAuthException.class)
    public ApiResponse<Void> handleAuthException(AdminAuthException exception) {
        return ApiResponse.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ApiResponse.error(500, exception.getMessage());
    }

    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class})
    public ApiResponse<Void> handleBindException(Exception exception) {
        logger.warn("Admin request parameter error", exception);
        return ApiResponse.error(500, "参数错误");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "参数错误" : error.getDefaultMessage())
                .collect(Collectors.joining("，"));
        return ApiResponse.error(500, message.isBlank() ? "参数错误" : message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        logger.error("Admin request failed", exception);
        String message = exception.getMessage();
        return ApiResponse.error(500, message == null || message.isBlank() ? "服务异常，请查看后台日志" : message);
    }
}
