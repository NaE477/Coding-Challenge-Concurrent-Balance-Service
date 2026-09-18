package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.model.TransactionRequest;
import com.code.challenge.mofid.service.DebitService;
import com.code.challenge.mofid.service.TransactionLedger;
import com.code.challenge.mofid.service.implementations.inmemory.model.InMemoryLockedAccount;
import com.code.challenge.mofid.validation.OperationValidator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InMemoryDebitService implements DebitService {

    private final InMemoryAccountStore accounts;
    private final TransactionLedger ledger;

    @Override
    public void debit(String accountId, long amount, String transactionId) {
        TransactionRequest request = OperationValidator.validateDebit(accountId, amount, transactionId);
        ledger.executeOperation(transactionId, request, () -> {
            InMemoryLockedAccount account = accounts.get(accountId);
            account.lock().lock();
            try {
                account.withdraw(amount);
            } finally {
                account.lock().unlock();
            }
        });
    }
}
