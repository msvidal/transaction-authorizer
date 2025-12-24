package com.github.msvidal.transactionauthorizer.infra.api.dto;

import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.domain.model.Transaction;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionResult;

public record TransactionResponse(
        TransactionDto transaction,
        AccountDto account,
        boolean webhookNotification
) {

    public static TransactionResponse toResponse(TransactionResult result) {
        return toResponse(result, false);
    }

    public static TransactionResponse toResponse(TransactionResult result, boolean webhookNotification) {
        return new TransactionResponse(
                toTransactionDto(result.transaction()),
                toAccountDto(result.account()),
                webhookNotification
        );
    }

    private static TransactionDto toTransactionDto(Transaction transaction) {
        return new TransactionDto(
                transaction.id(),
                transaction.operation().name(),
                new AmountDto(transaction.amount().value(), transaction.amount().currency().getCurrencyCode()),
                transaction.status().name(),
                transaction.createdAt());
    }

    private static AccountDto toAccountDto(Account account) {
        if (account == null) {
            return null;
        }
        return new AccountDto(account.id(),
                new AmountDto(account.balance().value(), account.balance().currency().getCurrencyCode())
        );
    }
}