package com.code.challenge.mofid.service;

import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.DeletedAccountException;
import com.code.challenge.mofid.exception.DuplicateAccountException;
import com.code.challenge.mofid.exception.InvalidAmountException;

public interface AccountRegistry {

    /**
     * @throws InvalidAmountException    if {@code initialBalance} is negative
     * @throws DuplicateAccountException if the account already exists
     * @throws DeletedAccountException   if the id belonged to a deleted account: ids are never reused
     */
    void openAccount(String accountId, long initialBalance);

    void deleteAccount(String accountId);
}
