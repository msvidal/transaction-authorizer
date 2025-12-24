package com.github.msvidal.transactionauthorizer.infra.messaging.dto;

import java.util.UUID;

public record TransactionCaptureMessage(
    UUID transactionId
) {}
