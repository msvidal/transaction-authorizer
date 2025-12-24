package com.github.msvidal.transactionauthorizer.domain.model;

public enum Status {
    PENDING,      // Requisição recebida
    AUTHORIZED,   // Saldo reservado (fase síncrona completa)
    CAPTURED,     // Movimentação efetivada (fase assíncrona completa)
    SETTLED,      // Conciliado/Finalizado
    SUCCEEDED,    // Legacy - mantido para compatibilidade
    FAILED,       // Falhou
    REVERSED;     // Estornado
}
