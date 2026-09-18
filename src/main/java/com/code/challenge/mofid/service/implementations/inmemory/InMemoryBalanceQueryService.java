package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.service.BalanceQueryService;
import com.code.challenge.mofid.service.implementations.inmemory.model.InMemoryLockedAccount;
import com.code.challenge.mofid.validation.OperationValidator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InMemoryBalanceQueryService implements BalanceQueryService {

    private final InMemoryAccountStore accounts;

    @Override
    public long getBalance(String accountId) {
        OperationValidator.validateAccountId(accountId);
        InMemoryLockedAccount account = accounts.get(accountId);
        account.lock().lock();
        try {
            return account.balance();
        } finally {
            account.lock().unlock();
        }
    }
}
