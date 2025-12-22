package com.github.msvidal.transactionauthorizer.infra.api.dto;

import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.domain.model.Transaction;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionResult;

import java.math.BigDecimal;

public record TransactionResponse(
        TransactionDto transaction,
        AccountDto account
) {

    public static TransactionResponse toResponse(TransactionResult result) {
        return new TransactionResponse(
                toTransactionDto(result.transaction()),
                toAccountDto(result.account())
        );
    }

    private static TransactionDto toTransactionDto(Transaction transaction) {
        BigDecimal value = transaction.amount().getNumber().numberValue(BigDecimal.class);
        String currencyCode = transaction.amount().getCurrency().getCurrencyCode();
        return new TransactionDto(
                transaction.id(),
                transaction.operation().name(),
                new AmountDto(value, currencyCode),
                transaction.status().name(),
                transaction.createdAt());
    }

    private static AccountDto toAccountDto(Account account) {
        if (account == null) {
            return null;
        }
        BigDecimal value = account.balance().getNumber().numberValue(BigDecimal.class);
        String currencyCode = account.balance().getCurrency().getCurrencyCode();
        return new AccountDto(account.id(),
                new AmountDto(value, currencyCode)
        );
    }
}