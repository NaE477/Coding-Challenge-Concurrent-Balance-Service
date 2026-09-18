package com.code.challenge.mofid.exception;

import lombok.Getter;

@Getter
public class DuplicateAccountException extends BalanceServiceException {

    private final String accountId;

    public DuplicateAccountException(String accountId) {
        super("Account '" + accountId + "' already exists");
        this.accountId = accountId;
    }
}
