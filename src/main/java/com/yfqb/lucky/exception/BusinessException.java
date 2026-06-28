package com.yfqb.lucky.exception;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * @author xuchengcheng
 * @since 2026-04-23
 *
 */
public class BusinessException extends RuntimeException{

    public static final int DEFAULT_CODE = 10000;
    /**
     * 所有业务错误异常
     */
    public static final BusinessException UNKNOWN = new BusinessException(DEFAULT_CODE,"未知错误");

    @Serial
    private static final long serialVersionUID = 1L;
    @Getter @Setter private int code;
    @Getter @Setter private String reason;

    public BusinessException(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }
}
