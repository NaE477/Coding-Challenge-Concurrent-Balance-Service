package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.model.TransactionRequest;
import com.code.challenge.mofid.service.TransactionLedger;
import com.code.challenge.mofid.service.TransferService;
import com.code.challenge.mofid.validation.OperationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(Profiles.POSTGRES)
@RequiredArgsConstructor
public class JdbcTransferService implements TransferService {

    private final TransactionLedger ledger;
    private final JdbcAccountBalances balances;

    @Override
    @Transactional
    public void transfer(String sourceAccountId, String destinationAccountId, long amount, String transactionId) {
        TransactionRequest request =
                OperationValidator.validateTransfer(sourceAccountId, destinationAccountId, amount, transactionId);
        ledger.executeOperation(transactionId, request,
                () -> updateBothAccountsInCharacterOrder(sourceAccountId, destinationAccountId, amount));
    }

    /**
     * Each UPDATE locks its row until the transaction ends, so updating in account-id order means
     * every transfer takes its row locks in the same order, and two transfers can't deadlock.
     */
    private void updateBothAccountsInCharacterOrder(String sourceAccountId, String destinationAccountId, long amount) {
        if (sourceAccountId.compareTo(destinationAccountId) < 0) {
            balances.withdraw(sourceAccountId, amount);
            balances.deposit(destinationAccountId, amount);
        } else {
            balances.deposit(destinationAccountId, amount);
            balances.withdraw(sourceAccountId, amount);
        }
    }
}
