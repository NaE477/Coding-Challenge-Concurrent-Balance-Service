package com.code.challenge.mofid.service.implementations.inmemory.model;

import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.BalanceOverflowException;
import com.code.challenge.mofid.exception.InsufficientFundsException;

import java.util.concurrent.locks.ReentrantLock;

/**
 * An account of the in-memory backend. Its balance may only be read or changed while holding its lock;
 * every method checks this and throws otherwise. A deleted account behaves as if it didn't exist.
 */
public final class InMemoryLockedAccount {

    private final String id;
    private final ReentrantLock lock = new ReentrantLock();
    private long balance;
    private boolean deleted;

    public InMemoryLockedAccount(String id, long initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }

    public String id() {
        return id;
    }

    public ReentrantLock lock() {
        return lock;
    }

    public long balance() {
        requireUsable();
        return balance;
    }

    public void requireFunds(long amount) {
        requireUsable();
        if (balance < amount) {
            throw new InsufficientFundsException(id, amount);
        }
    }

    public void requireRoomFor(long amount) {
        requireUsable();
        if (balance > Long.MAX_VALUE - amount) {
            throw new BalanceOverflowException(id, amount);
        }
    }

    public void withdraw(long amount) {
        requireFunds(amount);
        balance -= amount;
    }

    public void deposit(long amount) {
        requireRoomFor(amount);
        balance += amount;
    }

    public boolean isDeleted() {
        requireLockHeld();
        return deleted;
    }

    public void delete() {
        requireUsable();
        deleted = true;
    }

    private void requireUsable() {
        requireLockHeld();
        if (deleted) {
            throw new AccountNotFoundException(id);
        }
    }

    private void requireLockHeld() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Lock of account '" + id + "' is not held by the current thread");
        }
    }
}
