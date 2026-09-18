package com.code.challenge.mofid.exception;

import lombok.Getter;

@Getter
public class BalanceOverflowException extends BalanceServiceException {

    private final String accountId;
    private final long amount;

    public BalanceOverflowException(String accountId, long amount) {
        super("Crediting " + amount + " would overflow the balance of account '" + accountId + "'");
        this.accountId = accountId;
        this.amount = amount;
    }
}
