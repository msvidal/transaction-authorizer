package com.github.msvidal.transactionauthorizer.infra.api;

import com.github.msvidal.transactionauthorizer.infra.config.WebhookConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook-test")
@Tag(name = "Webhook Test", description = "Endpoint mock para testar webhooks localmente")
@AllArgsConstructor
@Slf4j
@Profile({"api", "local"})
public class WebhookTestController {

    private final WebhookConfig webhookConfig;

    @Operation(
            summary = "Receber webhook de teste",
            description = "Endpoint mock que recebe e valida webhooks para testes locais"
    )
    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String payload) {

        log.info("Webhook received - Payload length: {} bytes", payload.length());
        log.debug("Webhook payload: {}", payload);

        if (signature != null && !signature.isEmpty()) {
            boolean valid = validateSignature(payload, signature);
            log.info("Webhook signature validation: {}", valid ? "VALID" : "INVALID");
            
            if (!valid) {
                log.warn("Invalid webhook signature received");
                return ResponseEntity.status(401)
                        .body(Map.of("status", "error", "message", "Invalid signature"));
            }
        } else {
            log.warn("Webhook received without signature");
        }

        log.info("Webhook processed successfully");
        return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook received"));
    }

    private boolean validateSignature(String payload, String receivedSignature) {
        if (!receivedSignature.startsWith("sha256=")) {
            return false;
        }
        
        String receivedHash = receivedSignature.substring(7);
        String expectedHash = new HmacUtils("HmacSHA256", webhookConfig.getWebhookSecret()).hmacHex(payload);
        
        return receivedHash.equals(expectedHash);
    }
}
