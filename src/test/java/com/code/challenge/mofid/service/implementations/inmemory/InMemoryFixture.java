package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.service.*;
import com.code.challenge.mofid.service.implementations.BalanceServiceImpl;

/**
 * Wires the in-memory implementation the same way the application will.
 */
final class InMemoryFixture {

    final InMemoryAccountStore accounts = new InMemoryAccountStore();
    final InMemoryTransactionLedger ledger = new InMemoryTransactionLedger();
    final CreditService creditService = new InMemoryCreditService(accounts, ledger);
    final DebitService debitService = new InMemoryDebitService(accounts, ledger);
    final TransferService transferService = new InMemoryTransferService(accounts, ledger);
    final BalanceQueryService balanceQueryService = new InMemoryBalanceQueryService(accounts);
    final BalanceService balanceService =
            new BalanceServiceImpl(creditService, debitService, transferService, balanceQueryService);
}
