package com.github.msvidal.transactionauthorizer.infra.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.msvidal.transactionauthorizer.domain.model.Status;
import com.github.msvidal.transactionauthorizer.domain.model.WebhookEvent;
import com.github.msvidal.transactionauthorizer.infra.config.WebhookConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookNotificationAdapterTest {

    @Mock
    private WebhookConfig webhookConfig;

    @Mock
    private RestTemplate webhookRestTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private WebhookNotificationAdapter webhookNotificationAdapter;

    @BeforeEach
    void setUp() {
        webhookNotificationAdapter = new WebhookNotificationAdapter(
                webhookConfig,
                webhookRestTemplate,
                objectMapper
        );
    }

    @Test
    void shouldSendWebhookWithCorrectPayloadAndSignature() throws Exception {
        // Given
        WebhookEvent event = createWebhookEvent();
        String payload = "{\"eventId\":\"test\"}";

        when(webhookConfig.isWebhookEnabled()).thenReturn(true);
        when(webhookConfig.getWebhookUrl()).thenReturn("http://localhost:8080/webhook-test");
        when(webhookConfig.getWebhookSecret()).thenReturn("test-secret");
        when(webhookConfig.getMaxRetryAttempts()).thenReturn(3);
        when(objectMapper.writeValueAsString(event)).thenReturn(payload);
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("success"));

        // When
        webhookNotificationAdapter.notify(event);

        // Then
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(webhookRestTemplate).postForEntity(
                eq("http://localhost:8080/webhook-test"),
                entityCaptor.capture(),
                eq(String.class)
        );

        HttpEntity<String> capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getBody()).isEqualTo(payload);
        assertThat(capturedEntity.getHeaders().get("X-Webhook-Signature")).isNotNull();
        assertThat(capturedEntity.getHeaders().get("X-Webhook-Signature").get(0)).startsWith("sha256=");
    }

    @Test
    void shouldNotSendWebhookWhenDisabled() {
        // Given
        WebhookEvent event = createWebhookEvent();

        when(webhookConfig.isWebhookEnabled()).thenReturn(false);

        // When
        webhookNotificationAdapter.notify(event);

        // Then
        verify(webhookRestTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void shouldNotSendWebhookWhenUrlIsEmpty() {
        // Given
        WebhookEvent event = createWebhookEvent();

        when(webhookConfig.isWebhookEnabled()).thenReturn(true);
        when(webhookConfig.getWebhookUrl()).thenReturn("");

        // When
        webhookNotificationAdapter.notify(event);

        // Then
        verify(webhookRestTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void shouldRetryOnFailureUpToMaxAttempts() throws Exception {
        // Given
        WebhookEvent event = createWebhookEvent();
        String payload = "{\"eventId\":\"test\"}";

        when(webhookConfig.isWebhookEnabled()).thenReturn(true);
        when(webhookConfig.getWebhookUrl()).thenReturn("http://localhost:8080/webhook-test");
        when(webhookConfig.getWebhookSecret()).thenReturn("test-secret");
        when(webhookConfig.getMaxRetryAttempts()).thenReturn(3);
        when(webhookConfig.getRetryBackoffMs()).thenReturn(10L);
        when(objectMapper.writeValueAsString(event)).thenReturn(payload);
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection error"))
                .thenThrow(new RestClientException("Connection error"))
                .thenReturn(ResponseEntity.ok("success"));

        // When
        webhookNotificationAdapter.notify(event);

        // Then
        verify(webhookRestTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void shouldStopRetryingAfterMaxAttempts() throws Exception {
        // Given
        WebhookEvent event = createWebhookEvent();
        String payload = "{\"eventId\":\"test\"}";

        when(webhookConfig.isWebhookEnabled()).thenReturn(true);
        when(webhookConfig.getWebhookUrl()).thenReturn("http://localhost:8080/webhook-test");
        when(webhookConfig.getWebhookSecret()).thenReturn("test-secret");
        when(webhookConfig.getMaxRetryAttempts()).thenReturn(3);
        when(webhookConfig.getRetryBackoffMs()).thenReturn(10L);
        when(objectMapper.writeValueAsString(event)).thenReturn(payload);
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection error"));

        // When
        webhookNotificationAdapter.notify(event);

        // Then
        verify(webhookRestTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    private WebhookEvent createWebhookEvent() {
        return new WebhookEvent(
                UUID.randomUUID(),
                "transaction.captured",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Status.CAPTURED,
                OffsetDateTime.now(),
                Map.of("amount", 100.00, "currency", "BRL")
        );
    }
}
