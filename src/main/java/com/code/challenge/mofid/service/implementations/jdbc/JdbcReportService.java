package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.Profiles;
import com.code.challenge.mofid.model.AccountBalance;
import com.code.challenge.mofid.model.TransactionRecord;
import com.code.challenge.mofid.repository.AccountRepository;
import com.code.challenge.mofid.repository.TransactionRepository;
import com.code.challenge.mofid.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile(Profiles.POSTGRES)
@RequiredArgsConstructor
public class JdbcReportService implements ReportService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    @Override
    @Transactional(readOnly = true)
    public List<AccountBalance> accountBalances() {
        return accounts.findAllBalances();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionRecord> transactions() {
        return transactions.findAll();
    }
}
