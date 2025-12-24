package com.github.msvidal.transactionauthorizer.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebhookConfig {
    
    @Value("${webhook.url:}")
    private String webhookUrl;
    
    @Value("${webhook.secret:default-secret}")
    private String webhookSecret;
    
    @Value("${webhook.enabled:true}")
    private boolean webhookEnabled;

    @Value("${webhook.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${webhook.retry.backoff-ms:1000}")
    private long retryBackoffMs;

    @Value("${webhook.timeout-ms:5000}")
    private int timeoutMs;

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public boolean isWebhookEnabled() {
        return webhookEnabled;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    @Bean
    public RestTemplate webhookRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
