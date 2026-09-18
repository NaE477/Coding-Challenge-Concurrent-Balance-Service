package com.code.challenge.mofid.validation;

import com.code.challenge.mofid.exception.InvalidAmountException;
import com.code.challenge.mofid.exception.InvalidRequestException;
import com.code.challenge.mofid.exception.SameAccountTransferException;
import com.code.challenge.mofid.model.TransactionRequest;

public final class OperationValidator {

    public static final int MAX_ID_LENGTH = 64;
    public static final String ACCOUNT_ID = "accountId";
    public static final String TRANSACTION_ID = "transactionId";

    private OperationValidator() {
    }

    public static TransactionRequest validateCredit(String accountId, long amount, String transactionId) {
        requireId(ACCOUNT_ID, accountId);
        requireId(TRANSACTION_ID, transactionId);
        requirePositive(amount);
        return TransactionRequest.credit(accountId, amount);
    }

    public static TransactionRequest validateDebit(String accountId, long amount, String transactionId) {
        requireId(ACCOUNT_ID, accountId);
        requireId(TRANSACTION_ID, transactionId);
        requirePositive(amount);
        return TransactionRequest.debit(accountId, amount);
    }

    public static TransactionRequest validateTransfer(String sourceAccountId, String destinationAccountId,
                                                      long amount, String transactionId) {
        requireId("sourceAccountId", sourceAccountId);
        requireId("destinationAccountId", destinationAccountId);
        requireId(TRANSACTION_ID, transactionId);
        requirePositive(amount);
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new SameAccountTransferException(sourceAccountId);
        }
        return TransactionRequest.transfer(sourceAccountId, destinationAccountId, amount);
    }

    public static void validateOpenAccount(String accountId, long initialBalance) {
        requireId(ACCOUNT_ID, accountId);
        if (initialBalance < 0) {
            throw new InvalidAmountException("Initial balance must not be negative, was " + initialBalance);
        }
    }

    public static void validateAccountId(String accountId) {
        requireId(ACCOUNT_ID, accountId);
    }

    private static void requireId(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(name + " must not be blank");
        }
        if (value.length() > MAX_ID_LENGTH) {
            throw new InvalidRequestException(name + " must be at most " + MAX_ID_LENGTH + " characters");
        }
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be positive, was " + amount);
        }
    }
}
