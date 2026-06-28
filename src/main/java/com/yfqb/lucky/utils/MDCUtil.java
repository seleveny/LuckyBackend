package com.yfqb.lucky.utils;

import com.yfqb.lucky.constant.LogContextConstants;
import org.slf4j.MDC;
import reactor.util.context.Context;

/**
 * MDC 工具类 - 将 Reactor Context 同步到 Slf4j MDC
 */
public class MDCUtil {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 从 Reactor Context 同步 traceId 到 MDC
     */
    public static void syncToMDC(Context context) {
        String traceId = LogContextConstants.getTraceId(context);
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 清理 MDC
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * 设置 traceId 到 MDC
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }
}
