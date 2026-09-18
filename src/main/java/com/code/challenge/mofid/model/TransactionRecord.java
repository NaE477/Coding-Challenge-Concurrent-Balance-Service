package com.code.challenge.mofid.model;

import java.time.Instant;

public record TransactionRecord(
        String transactionId,
        TransactionType type,
        String sourceAccountId,
        String destinationAccountId,
        long amount,
        Instant recordedAt
) {
}
