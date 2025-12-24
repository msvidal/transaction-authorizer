package com.github.msvidal.transactionauthorizer.domain.model;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record WebhookEvent(
    UUID eventId,
    String eventType,  // "transaction.authorized", "transaction.captured", "transaction.failed"
    UUID transactionId,
    UUID accountId,
    Status status,
    OffsetDateTime timestamp,
    Map<String, Object> data
) {}
