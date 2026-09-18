package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.BalanceOverflowException;
import com.code.challenge.mofid.exception.InsufficientFundsException;
import com.code.challenge.mofid.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(Profiles.POSTGRES)
@RequiredArgsConstructor
class JdbcAccountBalances {

    private final AccountRepository accounts;

    void deposit(String accountId, long amount) {
        if (!accounts.deposit(accountId, amount)) {
            boolean exists = accounts.exists(accountId);
            throw exists
                    ? new BalanceOverflowException(accountId, amount)
                    : new AccountNotFoundException(accountId);
        }
    }

    void withdraw(String accountId, long amount) {
        if (!accounts.withdraw(accountId, amount)) {
            boolean exists = accounts.exists(accountId);
            throw exists
                    ? new InsufficientFundsException(accountId, amount)
                    : new AccountNotFoundException(accountId);
        }
    }
}
