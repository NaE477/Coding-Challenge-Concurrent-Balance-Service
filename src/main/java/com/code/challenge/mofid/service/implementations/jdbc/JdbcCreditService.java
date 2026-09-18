package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.model.TransactionRequest;
import com.code.challenge.mofid.service.CreditService;
import com.code.challenge.mofid.service.TransactionLedger;
import com.code.challenge.mofid.validation.OperationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(Profiles.POSTGRES)
@RequiredArgsConstructor
public class JdbcCreditService implements CreditService {

    private final TransactionLedger ledger;
    private final JdbcAccountBalances balances;

    @Override
    @Transactional
    public void credit(String accountId, long amount, String transactionId) {
        TransactionRequest request = OperationValidator.validateCredit(accountId, amount, transactionId);
        ledger.executeOperation(transactionId, request, () -> balances.deposit(accountId, amount));
    }
}
