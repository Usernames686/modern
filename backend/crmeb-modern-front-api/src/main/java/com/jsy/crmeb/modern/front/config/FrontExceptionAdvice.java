package com.jsy.crmeb.modern.front.config;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.front.FrontAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FrontExceptionAdvice {
    private static final Logger logger = LoggerFactory.getLogger(FrontExceptionAdvice.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.error(400, exception.getMessage());
    }

    @ExceptionHandler(FrontAuthException.class)
    public ApiResponse<Void> handleFrontAuth(FrontAuthException exception) {
        return ApiResponse.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        logger.error("Front request failed", exception);
        String message = exception.getMessage();
        return ApiResponse.error(500, message == null || message.isBlank() ? "服务异常，请查看后台日志" : message);
    }
}
