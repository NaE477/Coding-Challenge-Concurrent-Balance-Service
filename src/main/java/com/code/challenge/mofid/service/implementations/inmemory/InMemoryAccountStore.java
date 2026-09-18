package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.DuplicateAccountException;
import com.code.challenge.mofid.service.AccountRegistry;
import com.code.challenge.mofid.service.implementations.inmemory.model.InMemoryLockedAccount;
import com.code.challenge.mofid.validation.OperationValidator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAccountStore implements AccountRegistry {

    private final ConcurrentMap<String, InMemoryLockedAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public void openAccount(String accountId, long initialBalance) {
        OperationValidator.validateOpenAccount(accountId, initialBalance);
        if (accounts.putIfAbsent(accountId, new InMemoryLockedAccount(accountId, initialBalance)) != null) {
            throw new DuplicateAccountException(accountId);
        }
    }

    InMemoryLockedAccount get(String accountId) {
        InMemoryLockedAccount account = accounts.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException(accountId);
        }
        return account;
    }
}
