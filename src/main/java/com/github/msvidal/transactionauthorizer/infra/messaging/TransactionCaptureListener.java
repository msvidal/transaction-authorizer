package com.github.msvidal.transactionauthorizer.infra.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.msvidal.transactionauthorizer.application.usecase.CaptureTransactionUseCase;
import com.github.msvidal.transactionauthorizer.infra.messaging.dto.TransactionCaptureMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
@Profile({"listener", "local"})
public class TransactionCaptureListener {

    private final ObjectMapper objectMapper;
    private final CaptureTransactionUseCase captureTransactionUseCase;

    @SqsListener("${aws.sqs.capture-queue-name}")
    public void receive(String body) {
        try {
            log.info("Capture message received: {}", body);
            
            JsonNode root = objectMapper.readTree(body);
            
            // Support two message formats:
            // 1. Direct format: {"transactionId": "uuid"}
            // 2. Wrapped format: {"transaction": {"transactionId": "uuid"}}
            JsonNode messageNode = root.has("transactionId") ? root : 
                                  (root.has("transaction") ? root.get("transaction") : null);
            
            if (messageNode == null || messageNode.isMissingNode() || messageNode.isNull()) {
                log.error("Invalid message format: unable to locate transaction data in message");
                return;
            }

            TransactionCaptureMessage message = objectMapper.treeToValue(messageNode, TransactionCaptureMessage.class);
            
            if (message.transactionId() == null) {
                log.error("Transaction ID is null in capture message");
                return;
            }

            captureTransactionUseCase.execute(message.transactionId());
            
            log.info("Transaction capture processed successfully: {}", message.transactionId());

        } catch (Exception e) {
            log.error("Error processing capture message", e);
            throw new RuntimeException("Failed to process capture message", e);
        }
    }
}
