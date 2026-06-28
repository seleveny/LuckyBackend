package com.yfqb.lucky.webflux;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class RequestResponseLoggingFilter implements WebFilter {

    private static final int ERROR_STATUS_THRESHOLD = 400;
    private static final int MAX_BODY_LENGTH = 2048;

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // ⬇ exchange 在这里，整个 filter 方法都能访问
        ServerHttpRequest request = exchange.getRequest();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        long startTime = System.currentTimeMillis();

        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        boolean shouldLogReqBody = HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method);

        // ====== 第一步：读取并缓存请求体（把 exchange 传进去）======
        Mono<CachedBody> reqBodyMono = shouldLogReqBody
                ? cacheRequestBody(request, exchange)   // ← 修复：传入 exchange
                : Mono.just(new CachedBody(null, request));

        return reqBodyMono.flatMap(cachedReq -> {

            // ====== 第二步：装饰响应，拦截响应体写入 ======
            AtomicReference<String> responseBodyRef = new AtomicReference<>("");
            AtomicLong responseBodySize = new AtomicLong(0);

            ServerHttpResponse originalResponse = exchange.getResponse();
            ServerHttpResponse decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {

                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    Flux<DataBuffer> bufferFlux = Flux.from(body)
                            .map(this::captureAndProxy);
                    return super.writeWith(bufferFlux);
                }

                private DataBuffer captureAndProxy(DataBuffer original) {
                    int readableBytes = original.readableByteCount();
                    responseBodySize.addAndGet(readableBytes);

                    if (responseBodySize.get() <= MAX_BODY_LENGTH * 2) {
                        byte[] bytes = new byte[readableBytes];
                        original.read(bytes);
                        original.readPosition(0); // 复位读指针

                        String chunk = new String(bytes, StandardCharsets.UTF_8);
                        responseBodyRef.updateAndGet(existing ->
                                existing.length() < MAX_BODY_LENGTH
                                        ? existing + chunk
                                        : existing
                        );
                    }
                    return original;
                }
            };

            // ====== 第三步：执行过滤链 ======
            return chain.filter(
                            exchange.mutate()
                                    .request(cachedReq.decoratedRequest)
                                    .response(decoratedResponse)
                                    .build()
                    )
                    .contextWrite(reactor.util.context.Context.of("traceId", traceId))

                    // ====== 第四步：记录日志 ======
                    .doFinally(signalType -> {
                        long duration = System.currentTimeMillis() - startTime;
                        HttpStatusCode status = exchange.getResponse().getStatusCode();
                        int statusCode = status != null ? status.value() : -1;

                        LogContext ctx = new LogContext(
                                traceId, method, path,
                                cachedReq.bodyString,
                                responseBodyRef.get(),
                                duration,
                                statusCode
                        );

                        outputLog(ctx, signalType);
                    });
        }).onErrorResume(error -> {
            long duration = System.currentTimeMillis() - startTime;
            log.error("{}|{} {}|-|-|{}|errorMsg={}",
                    traceId, method, path, duration, error.getMessage());
            return Mono.error(error);
        });
    }

    // ==================== 缓存请求体（修复后）====================

    /**
     * 读取并缓存请求体。
     *
     * @param request  原始请求
     * @param exchange 服务端交换器（用于获取 DataBufferFactory）
     */
    private Mono<CachedBody> cacheRequestBody(ServerHttpRequest request,
                                              ServerWebExchange exchange) {  // ← 修复：新增参数
        final DataBufferFactory bufferFactory =
                exchange.getResponse().bufferFactory();  // ← 现在可以安全使用了

        return DataBufferUtils.join(request.getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    final byte[] cached = bytes;
                    ServerHttpRequest decorated = new ServerHttpRequestDecorator(request) {
                        @Override
                        @NullMarked
                        public Flux<DataBuffer> getBody() {
                            // ← 修复：直接用上面捕获的 bufferFactory，不再引用外部变量
                            return Flux.just(bufferFactory.wrap(cached));
                        }
                    };

                    String bodyStr = cached.length > 0
                            ? new String(cached, StandardCharsets.UTF_8)
                            : null;

                    return new CachedBody(bodyStr, decorated);
                })
                .switchIfEmpty(Mono.just(new CachedBody(null, request)))
                .onErrorResume(e -> {
                    log.warn("[{}] 读取请求体失败: {}", traceIdFallback(), e.getMessage());
                    return Mono.just(new CachedBody("[read_error]", request));
                });
    }

    // ==================== 日志输出 ====================

    private void outputLog(LogContext ctx, reactor.core.publisher.SignalType signalType) {
        if (signalType == reactor.core.publisher.SignalType.ON_ERROR) {
            log.error(formatLog(ctx));
        } else if (ctx.statusCode >= ERROR_STATUS_THRESHOLD) {
            log.error(formatLog(ctx));
        } else {
            log.info(formatLog(ctx));
        }
    }

    private String formatLog(LogContext ctx) {
        return String.format(
                "%s|%s %s|%s|%s|%d|[%d]",
                ctx.traceId, ctx.method, ctx.path,
                truncate(ctx.requestBody),
                truncate(ctx.responseBody),
                ctx.duration,
                ctx.statusCode
        );
    }

    private String truncate(String s) {
        if (s == null || s.isEmpty()) return "NULL";
        if (s.length() > MAX_BODY_LENGTH) {
            return s.substring(0, MAX_BODY_LENGTH) + "...[" + s.length() + "chars]";
        }
        return s;
    }

    private String traceIdFallback() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    // ==================== 内部类 ====================

    private static class CachedBody {
        final String bodyString;
        final ServerHttpRequest decoratedRequest;

        CachedBody(String bodyString, ServerHttpRequest decoratedRequest) {
            this.bodyString = bodyString;
            this.decoratedRequest = decoratedRequest;
        }
    }

    private static class LogContext {
        final String traceId;
        final String method;
        final String path;
        final String requestBody;
        final String responseBody;
        final long duration;
        final int statusCode;

        LogContext(String traceId, String method, String path,
                   String requestBody, String responseBody,
                   long duration, int statusCode) {
            this.traceId = traceId;
            this.method = method;
            this.path = path;
            this.requestBody = requestBody;
            this.responseBody = responseBody;
            this.duration = duration;
            this.statusCode = statusCode;
        }
    }
}
