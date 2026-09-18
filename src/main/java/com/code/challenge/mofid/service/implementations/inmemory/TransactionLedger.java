package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.exception.IdempotencyConflictException;
import com.code.challenge.mofid.model.TransactionRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Runs each transactionId's operation at most once. A duplicate waits for the first attempt and gets its result;
 * the same id with a different request is a conflict. A failed attempt frees the id for a retry.
 */
class TransactionLedger {

    private record TransactionEntry(TransactionRequest request, CompletableFuture<RuntimeException> outcome) {
    }

    private final ConcurrentMap<String, TransactionEntry> entries = new ConcurrentHashMap<>();

    void executeOperation(String transactionId, TransactionRequest request, Runnable operation) {
        TransactionEntry newTransactionEntry = new TransactionEntry(request, new CompletableFuture<>());
        TransactionEntry earlierTransactionEntry = entries.putIfAbsent(transactionId, newTransactionEntry);
        if (earlierTransactionEntry == null) { // Fresh transaction
            runAndPublishOutcome(transactionId, newTransactionEntry, operation);
        } else if (earlierTransactionEntry.request().equals(request)) { // Duplicated failed transaction
            returnOutcomeOf(earlierTransactionEntry);
        } else {
            throw new IdempotencyConflictException(transactionId);
        }
    }

    private void runAndPublishOutcome(String transactionId, TransactionEntry transactionEntry, Runnable operation) {
        try {
            operation.run();
            transactionEntry.outcome().complete(null);
        } catch (RuntimeException failure) {
            entries.remove(transactionId, transactionEntry);
            transactionEntry.outcome().complete(failure);
            throw failure;
        }
    }

    private static void returnOutcomeOf(TransactionEntry earlierTransactionEntry) {
        RuntimeException failure = earlierTransactionEntry.outcome().join();
        if (failure != null) {
            throw failure;
        }
    }
}
