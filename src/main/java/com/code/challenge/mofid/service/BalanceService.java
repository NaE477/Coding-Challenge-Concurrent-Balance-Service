package com.code.challenge.mofid.service;

import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.InsufficientFundsException;
import com.code.challenge.mofid.exception.SameAccountTransferException;

/**
 * Account balance operations, as specified by the challenge brief.
 */
public interface BalanceService {

    /**
     * @throws AccountNotFoundException if the account does not exist
     */
    void credit(String accountId, long amount, String transactionId);

    /**
     * Fails without changing the balance if the account holds less than {@code amount}.
     *
     * @throws AccountNotFoundException   if the account does not exist
     * @throws InsufficientFundsException if the balance is lower than {@code amount}
     */
    void debit(String accountId, long amount, String transactionId);

    /**
     * Moves {@code amount} atomically: either both balances change or neither does.
     *
     * @throws SameAccountTransferException if source and destination are the same account
     * @throws AccountNotFoundException     if either account does not exist
     * @throws InsufficientFundsException   if the source balance is lower than {@code amount}
     */
    void transfer(String sourceAccountId, String destinationAccountId, long amount, String transactionId);

    /**
     * @throws AccountNotFoundException if the account does not exist
     */
    long getBalance(String accountId);
}
