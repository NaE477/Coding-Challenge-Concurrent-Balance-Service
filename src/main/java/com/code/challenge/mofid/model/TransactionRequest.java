package com.code.challenge.mofid.model;

public record TransactionRequest(
        TransactionType type,
        String sourceAccountId,
        String destinationAccountId,
        long amount
) {

    public static TransactionRequest credit(String accountId, long amount) {
        return new TransactionRequest(TransactionType.CREDIT, null, accountId, amount);
    }

    public static TransactionRequest debit(String accountId, long amount) {
        return new TransactionRequest(TransactionType.DEBIT, accountId, null, amount);
    }

    public static TransactionRequest transfer(String sourceAccountId, String destinationAccountId, long amount) {
        return new TransactionRequest(TransactionType.TRANSFER, sourceAccountId, destinationAccountId, amount);
    }
}
