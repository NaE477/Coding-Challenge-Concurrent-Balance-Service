package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.repository.AccountRepository;
import com.code.challenge.mofid.service.BalanceQueryService;
import com.code.challenge.mofid.validation.OperationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(Profiles.POSTGRES)
@RequiredArgsConstructor
public class JdbcBalanceQueryService implements BalanceQueryService {

    private final AccountRepository accounts;

    @Override
    @Transactional(readOnly = true)
    public long getBalance(String accountId) {
        OperationValidator.validateAccountId(accountId);
        return accounts.findBalance(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
