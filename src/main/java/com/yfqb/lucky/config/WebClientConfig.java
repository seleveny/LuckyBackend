package com.yfqb.lucky.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * WebClient 配置
 * <p>
 * 提供通用 WebClient.Builder（无默认头部），以及福彩网专用的 WebClient。
 * 其他服务注入 WebClient.Builder 自行构建，福彩同步注入 cwlWebClient。
 */
@Configuration
public class WebClientConfig {

    /**
     * 基础 HttpClient，开启 gzip 自动解压
     */
    private HttpClient baseHttpClient() {
        return HttpClient.create()
                .compress(true) // 自动处理 gzip 解压
                .wiretap(true);
    }

    /**
     * 通用 WebClient.Builder
     * <p>
     * 仅配置连接器和编解码大小，不绑定任何默认头部，适用于请求任意第三方 API。
     * 如果需要自定义头部，注入此 Bean 后自行调用 .defaultHeader(...) 再 .build()。
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(baseHttpClient()))
                .codecs(config -> config
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)); // 16MB
    }

    /**
     * 通用 WebClient（无默认头部）
     * <p>
     * 如果只是简单 GET/POST 请求，不需要自定义头部，直接注入此 Bean 即可。
     * 注意：此 Bean 不携带任何默认请求头，适用于请求一般第三方 API。
     */
    @Bean
    public WebClient webClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.build();
    }

    /**
     * 福彩网专用 WebClient
     * <p>
     * 携带浏览器级别的请求头和 Cookie，用于绕过 cwl.gov.cn 的反爬检测。
     * Cookie 中的 HMF_CI 和 21_vq 来自浏览器实际请求，过期后需更新。
     * 注意：此 Bean 仅用于拉取双色球数据，其他服务请注入 {@link #webClient} 或 {@link #webClientBuilder}。
     */
    @Bean
    public WebClient cwlWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .defaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                .defaultHeader("Referer", "https://www.cwl.gov.cn/ygkj/wqkjgg/ssq/")
                .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .defaultHeader("Accept-Language", "zh-CN,zh;q=0.9")
                .defaultHeader("Cache-Control", "max-age=0")
                .defaultHeader("Connection", "keep-alive")
                .defaultHeader("Sec-Fetch-Dest", "document")
                .defaultHeader("Sec-Fetch-Mode", "navigate")
                .defaultHeader("Sec-Fetch-Site", "none")
                .defaultHeader("Sec-Fetch-User", "?1")
                .defaultHeader("Upgrade-Insecure-Requests", "1")
                .defaultHeader("sec-ch-ua", "\"Google Chrome\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"")
                .defaultHeader("sec-ch-ua-mobile", "?0")
                .defaultHeader("sec-ch-ua-platform", "\"macOS\"")
                .defaultCookie("HMF_CI", "1204b64891a9437605c022529def2b9fc1f30c768a8defe861a4ddf9e89ae733f6a691f7ba66dccaaff2b92bbadb4851e3c873404914ef162e1e557b6fb6ff9a68")
                .defaultCookie("21_vq", "5")
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
