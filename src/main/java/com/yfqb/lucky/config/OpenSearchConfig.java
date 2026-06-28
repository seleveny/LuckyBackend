package com.yfqb.lucky.config;

import lombok.Getter;
import lombok.Setter;
import java.net.URI;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "spring.opensearch")
public class OpenSearchConfig {

    private String uris;
    private int connectionTimeout = 5000;
    private int socketTimeout = 60000;

    @Bean
    public OpenSearchClient openSearchClient() throws Exception {
        HttpHost host = HttpHost.create(URI.create(uris));

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .build();

        PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(10)
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(host);
        builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setConnectionManager(connectionManager)
        );
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder.setResponseTimeout(Timeout.ofMilliseconds(socketTimeout))
        );

        OpenSearchTransport transport = builder.build();
        return new OpenSearchClient(transport);
    }
}
