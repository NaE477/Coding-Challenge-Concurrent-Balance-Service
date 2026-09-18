package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.model.AccountBalance;
import com.code.challenge.mofid.model.TransactionRecord;
import com.code.challenge.mofid.service.ReportService;
import com.code.challenge.mofid.service.implementations.inmemory.model.InMemoryLockedAccount;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
class InMemoryReportService implements ReportService {

    private final InMemoryAccountStore accounts;
    private final InMemoryTransactionLedger ledger;

    @Override
    public List<AccountBalance> accountBalances() {
        List<InMemoryLockedAccount> ordered = accounts.all().stream()
                .sorted(Comparator.comparing(InMemoryLockedAccount::id))
                .toList();
        ordered.forEach(account -> account.lock().lock());
        try {
            return ordered.stream()
                    .filter(account -> !account.isDeleted())
                    .map(account -> new AccountBalance(account.id(), account.balance()))
                    .toList();
        } finally {
            for (int i = ordered.size() - 1; i >= 0; i--) {
                ordered.get(i).lock().unlock();
            }
        }
    }

    @Override
    public List<TransactionRecord> transactions() {
        return ledger.appliedTransactions();
    }
}
