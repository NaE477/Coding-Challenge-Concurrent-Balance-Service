package com.code.challenge.mofid.service;

import com.code.challenge.mofid.exception.DuplicateAccountException;
import com.code.challenge.mofid.exception.InvalidAmountException;

public interface AccountRegistry {

    /**
     * @throws InvalidAmountException    if {@code initialBalance} is negative
     * @throws DuplicateAccountException if the account already exists
     */
    void openAccount(String accountId, long initialBalance);

    boolean exists(String accountId);
}
