package com.code.challenge.mofid.exception;

import lombok.Getter;

@Getter
public class AccountNotFoundException extends BalanceServiceException {

    private final String accountId;

    public AccountNotFoundException(String accountId) {
        super("Account '" + accountId + "' does not exist");
        this.accountId = accountId;
    }
}
