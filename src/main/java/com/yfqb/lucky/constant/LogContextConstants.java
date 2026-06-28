package com.yfqb.lucky.constant;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

/**
 * 日志上下文常量
 */
public class LogContextConstants {

    public static final String TRACE_ID = "traceId";
    public static final String API_METHOD = "apiMethod";
    public static final String API_PATH = "apiPath";

    /**
     * 从 ContextView 中获取 traceId
     */
    public static String getTraceId(ContextView context) {
        return context.getOrEmpty(TRACE_ID)
                .map(Object::toString)
                .orElse("N/A");
    }

    /**
     * 构建包含 traceId 的 Reactor Context
     */
    public static Context withTraceId(String traceId) {
        return Context.of(TRACE_ID, traceId);
    }
}
