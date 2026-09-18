package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.model.TransactionRequest;
import com.code.challenge.mofid.service.TransferService;
import com.code.challenge.mofid.service.implementations.inmemory.model.InMemoryLockedAccount;
import com.code.challenge.mofid.validation.OperationValidator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InMemoryTransferService implements TransferService {

    private final InMemoryAccountStore accounts;
    private final TransactionLedger ledger;

    @Override
    public void transfer(String sourceAccountId, String destinationAccountId, long amount, String transactionId) {
        TransactionRequest request = OperationValidator.validateTransfer(sourceAccountId, destinationAccountId, amount, transactionId);
        ledger.executeOperation(transactionId, request, () -> {
            InMemoryLockedAccount source = accounts.get(sourceAccountId);
            InMemoryLockedAccount destination = accounts.get(destinationAccountId);
            withBothLocksInCharacterOrder(source, destination, () -> {
                // Locks don't roll back: check everything before changing anything, so a failure leaves both untouched.
                source.requireFunds(amount);
                destination.requireRoomFor(amount);
                source.withdraw(amount);
                destination.deposit(amount);
            });
        });
    }

    /**
     * Locks are always taken based on accounts' ids so the ordering is always the same.
     */
    private static void withBothLocksInCharacterOrder(InMemoryLockedAccount a, InMemoryLockedAccount b, Runnable action) {
        boolean aFirst = a.id().compareTo(b.id()) < 0;
        InMemoryLockedAccount first = aFirst ? a : b;
        InMemoryLockedAccount second = aFirst ? b : a;

        first.lock().lock();
        try {
            second.lock().lock();
            try {
                action.run();
            } finally {
                second.lock().unlock();
            }
        } finally {
            first.lock().unlock();
        }
    }
}
