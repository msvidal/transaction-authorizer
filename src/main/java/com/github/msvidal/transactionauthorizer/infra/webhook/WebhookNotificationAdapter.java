package com.github.msvidal.transactionauthorizer.infra.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.msvidal.transactionauthorizer.domain.model.WebhookEvent;
import com.github.msvidal.transactionauthorizer.domain.service.WebhookNotificationService;
import com.github.msvidal.transactionauthorizer.infra.config.WebhookConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
@Slf4j
public class WebhookNotificationAdapter implements WebhookNotificationService {

    private final WebhookConfig webhookConfig;
    private final RestTemplate webhookRestTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void notify(WebhookEvent event) {
        if (!webhookConfig.isWebhookEnabled()) {
            log.debug("Webhook notifications are disabled");
            return;
        }

        if (webhookConfig.getWebhookUrl() == null || webhookConfig.getWebhookUrl().isEmpty()) {
            log.warn("Webhook URL is not configured, skipping notification for event: {}", event.eventId());
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            sendWebhookWithRetry(payload, event.eventId().toString());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook event: {}", event.eventId(), e);
        }
    }

    private void sendWebhookWithRetry(String payload, String eventId) {
        int maxAttempts = webhookConfig.getMaxRetryAttempts();
        long backoffMs = webhookConfig.getRetryBackoffMs();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                sendWebhook(payload);
                log.info("Webhook sent successfully for event: {} on attempt {}", eventId, attempt);
                return;
            } catch (Exception e) {
                log.warn("Webhook send failed for event: {} on attempt {}/{}: {}", 
                        eventId, attempt, maxAttempts, e.getMessage());
                
                if (attempt < maxAttempts) {
                    try {
                        // Note: Using Thread.sleep() for simplicity. For production, consider using
                        // asynchronous retry mechanisms like Spring Retry with @Async or reactive approaches
                        long sleepTime = backoffMs * (long) Math.pow(2, attempt - 1);
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Webhook retry interrupted for event: {}", eventId);
                        return;
                    }
                }
            }
        }
        
        log.error("Failed to send webhook after {} attempts for event: {}", maxAttempts, eventId);
    }

    private void sendWebhook(String payload) {
        String signature = generateSignature(payload);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Signature", "sha256=" + signature);
        
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        
        webhookRestTemplate.postForEntity(
                webhookConfig.getWebhookUrl(),
                request,
                String.class
        );
    }

    private String generateSignature(String payload) {
        return new HmacUtils("HmacSHA256", webhookConfig.getWebhookSecret()).hmacHex(payload);
    }
}
