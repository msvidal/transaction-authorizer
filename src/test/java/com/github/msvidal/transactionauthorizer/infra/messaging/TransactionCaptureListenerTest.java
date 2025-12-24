package com.github.msvidal.transactionauthorizer.infra.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.msvidal.transactionauthorizer.application.usecase.CaptureTransactionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionCaptureListenerTest {

    private ObjectMapper objectMapper;

    @Mock
    private CaptureTransactionUseCase captureTransactionUseCase;

    private TransactionCaptureListener transactionCaptureListener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        transactionCaptureListener = new TransactionCaptureListener(objectMapper, captureTransactionUseCase);
    }

    @Test
    void shouldProcessValidCaptureMessage() {
        // Given
        UUID transactionId = UUID.randomUUID();
        String messageBody = "{\"transactionId\":\"" + transactionId + "\"}";
        
        doNothing().when(captureTransactionUseCase).execute(transactionId);

        // When
        transactionCaptureListener.receive(messageBody);

        // Then
        verify(captureTransactionUseCase).execute(transactionId);
    }

    @Test
    void shouldNotCallUseCaseWhenTransactionIdIsNull() {
        // Given
        String messageBody = "{\"transactionId\":null}";

        // When
        transactionCaptureListener.receive(messageBody);

        // Then
        verify(captureTransactionUseCase, never()).execute(any());
    }

    @Test
    void shouldThrowExceptionWhenCaptureUseCaseFails() {
        // Given
        UUID transactionId = UUID.randomUUID();
        String messageBody = "{\"transactionId\":\"" + transactionId + "\"}";
        
        doThrow(new RuntimeException("Capture failed")).when(captureTransactionUseCase).execute(transactionId);

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            transactionCaptureListener.receive(messageBody);
        });
    }
}
