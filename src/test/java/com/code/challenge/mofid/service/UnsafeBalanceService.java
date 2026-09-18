package com.code.challenge.mofid.service;

import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.DuplicateAccountException;
import com.code.challenge.mofid.exception.InsufficientFundsException;
import com.code.challenge.mofid.validation.OperationValidator;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A balance service broken on purpose, for ContractTeethTest only. It behaves correctly except for
 * the one Flaw it is created with. Only one flaw at a time: two flaws can hide each other.
 */
class UnsafeBalanceService implements BalanceService, AccountRegistry {

    enum Flaw {
        LOST_UPDATES,
        NON_ATOMIC_DEDUPLICATION
    }

    private final Flaw flaw;
    private final Map<String, Long> balances = new ConcurrentHashMap<>();
    private final Set<String> seenTransactions = ConcurrentHashMap.newKeySet();

    UnsafeBalanceService(Flaw flaw) {
        this.flaw = flaw;
    }

    @Override
    public void openAccount(String accountId, long initialBalance) {
        if (balances.putIfAbsent(accountId, initialBalance) != null) {
            throw new DuplicateAccountException(accountId);
        }
    }

    @Override
    public void credit(String accountId, long amount, String transactionId) {
        OperationValidator.validateCredit(accountId, amount, transactionId);
        if (alreadySeen(transactionId)) {
            return;
        }
        add(accountId, amount);
    }

    @Override
    public void debit(String accountId, long amount, String transactionId) {
        OperationValidator.validateDebit(accountId, amount, transactionId);
        if (alreadySeen(transactionId)) {
            return;
        }
        subtract(accountId, amount, transactionId);
    }

    @Override
    public void transfer(String sourceAccountId, String destinationAccountId, long amount, String transactionId) {
        OperationValidator.validateTransfer(sourceAccountId, destinationAccountId, amount, transactionId);
        if (alreadySeen(transactionId)) {
            return;
        }
        subtract(sourceAccountId, amount, transactionId);
        add(destinationAccountId, amount);
    }

    @Override
    public long getBalance(String accountId) {
        return read(accountId);
    }

    private boolean alreadySeen(String transactionId) {
        if (flaw != Flaw.NON_ATOMIC_DEDUPLICATION) {
            return !seenTransactions.add(transactionId);
        }
        if (seenTransactions.contains(transactionId)) {
            return true;
        }
        widenRaceWindow();
        seenTransactions.add(transactionId);
        return false;
    }

    private void add(String accountId, long amount) {
        if (flaw != Flaw.LOST_UPDATES) {
            synchronized (balances) {
                balances.put(accountId, read(accountId) + amount);
            }
            return;
        }
        long balance = read(accountId);
        widenRaceWindow();
        balances.put(accountId, balance + amount);
    }

    private void subtract(String accountId, long amount, String transactionId) {
        if (flaw != Flaw.LOST_UPDATES) {
            synchronized (balances) {
                long balance = read(accountId);
                requireFunds(accountId, amount, transactionId, balance);
                balances.put(accountId, balance - amount);
            }
            return;
        }
        long balance = read(accountId);
        requireFunds(accountId, amount, transactionId, balance);
        widenRaceWindow();
        balances.put(accountId, balance - amount);
    }

    private void requireFunds(String accountId, long amount, String transactionId, long balance) {
        if (balance < amount) {
            seenTransactions.remove(transactionId);
            throw new InsufficientFundsException(accountId, amount);
        }
    }

    private long read(String accountId) {
        Long balance = balances.get(accountId);
        if (balance == null) {
            throw new AccountNotFoundException(accountId);
        }
        return balance;
    }

    /** Hands the CPU to other threads at the worst moment, so the race shows up reliably. */
    private static void widenRaceWindow() {
        for (int i = 0; i < 50; i++) {
            Thread.yield();
        }
    }
}
