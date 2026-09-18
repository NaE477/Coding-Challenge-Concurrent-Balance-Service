package com.code.challenge.mofid.service.implementations;

import com.code.challenge.mofid.service.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

    private final CreditService creditService;
    private final DebitService debitService;
    private final TransferService transferService;
    private final BalanceQueryService balanceQueryService;

    @Override
    public void credit(String accountId, long amount, String transactionId) {
        creditService.credit(accountId, amount, transactionId);
    }

    @Override
    public void debit(String accountId, long amount, String transactionId) {
        debitService.debit(accountId, amount, transactionId);
    }

    @Override
    public void transfer(String sourceAccountId, String destinationAccountId, long amount, String transactionId) {
        transferService.transfer(sourceAccountId, destinationAccountId, amount, transactionId);
    }

    @Override
    public long getBalance(String accountId) {
        return balanceQueryService.getBalance(accountId);
    }
}
