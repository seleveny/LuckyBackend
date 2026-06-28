package com.yfqb.lucky.exception;

import com.yfqb.lucky.basic.IResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;
import org.springframework.dao.DataAccessException;
import io.r2dbc.spi.R2dbcException;


import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 自动捕获所有 Service/Repository 的异常，记录到 error.log
 */
@RestControllerAdvice
@Order(-1) // 确保最高优先级
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Throwable.class)
    public Mono<IResult<Void>> handleException(Throwable ex, ServerHttpRequest request) {
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        // 记录详细错误日志
        log.error("EXCEPTION|{} {}|{}|{}",
                method,
                path,
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex); // 打印完整堆栈

        return IResult.error("系统错误: " + ex.getMessage());
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<IResult<Void>> handleValidationException(WebExchangeBindException ex, ServerHttpRequest request) {
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("VALIDATION_ERROR|{} {}|{}", method, path, errors);

        return IResult.error("参数验证失败: " + errors);
    }

    /**
     * 处理业务异常（自定义异常）
     */
    @ExceptionHandler(BusinessException.class)
    public Mono<IResult<Void>> handleBusinessException(BusinessException ex, ServerHttpRequest request) {
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        // 业务异常只记录警告级别
        log.warn("BUSINESS_ERROR|{} {}|[{}]|{}",
                method, path, ex.getCode(), ex.getMessage());

        return IResult.error(ex.getMessage());
    }

    /**
     * 处理数据库相关异常
     */
    @ExceptionHandler(DataAccessException.class)
    public Mono<IResult<Void>> handleDataAccessException(
            DataAccessException ex,
            ServerHttpRequest request) {

        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        log.error("DATABASE_ERROR|{} {}|{}", method, path, ex.getMessage(), ex);

        return IResult.error("数据库操作失败");
    }

    /**
     * 处理 R2DBC 异常
     */
    @ExceptionHandler(R2dbcException.class)
    public Mono<IResult<Void>> handleR2dbcException(
            R2dbcException ex,
            ServerHttpRequest request) {

        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        log.error("R2DBC_ERROR|{} {}|{}", method, path, ex.getMessage(), ex);

        return IResult.error("数据库操作失败");
    }
}
