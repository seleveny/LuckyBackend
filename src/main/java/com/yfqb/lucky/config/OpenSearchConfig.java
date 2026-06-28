package com.yfqb.lucky.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.apachehttpclient5.ApacheHttpClient5TransportBuilder;
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
    public OpenSearchClient openSearchClient() {
        HttpHost host = HttpHost.create(uris);

        PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxTotalConnections(50)
                .setDefaultMaxPerRoute(10)
                .build();

        ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(host);
        builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setConnectionManager(connectionManager)
        );
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout(connectionTimeout)
                        .setResponseTimeout(socketTimeout)
        );

        OpenSearchTransport transport = builder.build();
        return new OpenSearchClient(transport);
    }
}
