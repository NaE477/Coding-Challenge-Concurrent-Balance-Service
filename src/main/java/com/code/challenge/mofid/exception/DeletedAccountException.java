package com.code.challenge.mofid.exception;

import lombok.Getter;

@Getter
public class DeletedAccountException extends BalanceServiceException {

    private final String accountId;

    public DeletedAccountException(String accountId) {
        super("Account '" + accountId + "' was deleted; its id can't be reused");
        this.accountId = accountId;
    }
}
