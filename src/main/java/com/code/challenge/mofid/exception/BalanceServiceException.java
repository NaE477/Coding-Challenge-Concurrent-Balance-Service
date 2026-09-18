package com.code.challenge.mofid.exception;

public abstract class BalanceServiceException extends RuntimeException {

    protected BalanceServiceException(String message) {
        super(message);
    }
}
