package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.DeletedAccountException;
import com.code.challenge.mofid.exception.DuplicateAccountException;
import com.code.challenge.mofid.repository.AccountRepository;
import com.code.challenge.mofid.service.AccountRegistry;
import com.code.challenge.mofid.validation.OperationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(Profiles.POSTGRES)
@RequiredArgsConstructor
public class JdbcAccountRegistry implements AccountRegistry {

    private final AccountRepository accounts;

    @Override
    @Transactional
    public void openAccount(String accountId, long initialBalance) {
        OperationValidator.validateOpenAccount(accountId, initialBalance);
        if (!accounts.insertIfAbsent(accountId, initialBalance)) {
            throw accounts.isDeleted(accountId)
                    ? new DeletedAccountException(accountId)
                    : new DuplicateAccountException(accountId);
        }
    }

    @Override
    @Transactional
    public void deleteAccount(String accountId) {
        OperationValidator.validateAccountId(accountId);
        if (!accounts.markDeleted(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
    }
}
