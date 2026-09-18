package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.DeletedAccountException;
import com.code.challenge.mofid.exception.DuplicateAccountException;
import com.code.challenge.mofid.service.AccountRegistry;
import com.code.challenge.mofid.service.implementations.inmemory.model.InMemoryLockedAccount;
import com.code.challenge.mofid.validation.OperationValidator;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAccountStore implements AccountRegistry {

    private final ConcurrentMap<String, InMemoryLockedAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public void openAccount(String accountId, long initialBalance) {
        OperationValidator.validateOpenAccount(accountId, initialBalance);
        InMemoryLockedAccount existing = accounts.putIfAbsent(accountId, new InMemoryLockedAccount(accountId, initialBalance));
        if (existing != null) {
            throw isDeleted(existing) ? new DeletedAccountException(accountId) : new DuplicateAccountException(accountId);
        }
    }

    /** The account stays in the map as a tombstone, so its id can never be opened again. */
    @Override
    public void deleteAccount(String accountId) {
        OperationValidator.validateAccountId(accountId);
        InMemoryLockedAccount account = get(accountId);
        account.lock().lock();
        try {
            account.delete();
        } finally {
            account.lock().unlock();
        }
    }

    private static boolean isDeleted(InMemoryLockedAccount account) {
        account.lock().lock();
        try {
            return account.isDeleted();
        } finally {
            account.lock().unlock();
        }
    }

    Collection<InMemoryLockedAccount> all() {
        return accounts.values();
    }

    InMemoryLockedAccount get(String accountId) {
        InMemoryLockedAccount account = accounts.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException(accountId);
        }
        return account;
    }
}
