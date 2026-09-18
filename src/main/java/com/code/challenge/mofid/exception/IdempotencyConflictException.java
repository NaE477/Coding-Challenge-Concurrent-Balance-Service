package com.code.challenge.mofid.exception;

import lombok.Getter;

/**
 * A transactionId was reused with a different request than the one it was first recorded with.
 */
@Getter
public class IdempotencyConflictException extends BalanceServiceException {

    private final String transactionId;

    public IdempotencyConflictException(String transactionId) {
        super("Transaction '" + transactionId + "' was already used for a different request");
        this.transactionId = transactionId;
    }
}
