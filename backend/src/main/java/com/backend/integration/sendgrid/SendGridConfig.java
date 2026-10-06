package com.backend.integration.sendgrid;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SendGridProperties.class)
public class SendGridConfig {

    @Bean(name = "sendGridRestClient")
    public RestClient sendGridRestClient(RestClient.Builder builder, SendGridProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        String baseUrl = properties.getApiBaseUrl() == null || properties.getApiBaseUrl().isBlank()
                ? "https://api.sendgrid.com/v3"
                : properties.getApiBaseUrl().replaceAll("/$", "");
        return builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
