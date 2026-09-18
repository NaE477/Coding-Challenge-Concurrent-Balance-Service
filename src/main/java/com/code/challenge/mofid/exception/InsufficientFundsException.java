package com.code.challenge.mofid.exception;

import lombok.Getter;

@Getter
public class InsufficientFundsException extends BalanceServiceException {

    private final String accountId;
    private final long amount;

    public InsufficientFundsException(String accountId, long amount) {
        super("Account '" + accountId + "' has insufficient funds for " + amount);
        this.accountId = accountId;
        this.amount = amount;
    }
}
