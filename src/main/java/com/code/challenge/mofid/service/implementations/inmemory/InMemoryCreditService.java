package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.model.TransactionRequest;
import com.code.challenge.mofid.service.CreditService;
import com.code.challenge.mofid.service.TransactionLedger;
import com.code.challenge.mofid.service.implementations.inmemory.model.InMemoryLockedAccount;
import com.code.challenge.mofid.validation.OperationValidator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InMemoryCreditService implements CreditService {

    private final InMemoryAccountStore accounts;
    private final TransactionLedger ledger;

    @Override
    public void credit(String accountId, long amount, String transactionId) {
        TransactionRequest request = OperationValidator.validateCredit(accountId, amount, transactionId);
        ledger.executeOperation(transactionId, request, () -> {
            InMemoryLockedAccount account = accounts.get(accountId);
            account.lock().lock();
            try {
                account.deposit(amount);
            } finally {
                account.lock().unlock();
            }
        });
    }
}
