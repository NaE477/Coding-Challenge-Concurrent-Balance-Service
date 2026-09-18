package com.code.challenge.mofid.controller;

import com.code.challenge.mofid.controller.dtos.report.AccountsReport;
import com.code.challenge.mofid.controller.dtos.report.TransactionsReport;
import com.code.challenge.mofid.model.AccountBalance;
import com.code.challenge.mofid.model.TransactionRecord;
import com.code.challenge.mofid.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/reports/accounts")
    public AccountsReport accounts() {
        List<AccountBalance> accounts = reportService.accountBalances();
        BigInteger total = accounts.stream()
                .map(account -> BigInteger.valueOf(account.balance()))
                .reduce(BigInteger.ZERO, BigInteger::add);
        return new AccountsReport(accounts.size(), total, accounts);
    }

    @GetMapping("/reports/transactions")
    public TransactionsReport transactions() {
        List<TransactionRecord> transactions = reportService.transactions();
        return new TransactionsReport(transactions.size(), transactions);
    }
}
