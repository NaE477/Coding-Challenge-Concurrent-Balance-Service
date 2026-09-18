package com.code.challenge.mofid.exception;

import lombok.Getter;

@Getter
public class SameAccountTransferException extends BalanceServiceException {

    private final String accountId;

    public SameAccountTransferException(String accountId) {
        super("Cannot transfer from account '" + accountId + "' to itself");
        this.accountId = accountId;
    }
}
