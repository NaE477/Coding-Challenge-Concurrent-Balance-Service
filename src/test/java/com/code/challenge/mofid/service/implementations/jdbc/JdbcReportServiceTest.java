package com.code.challenge.mofid.service.implementations.jdbc;

import com.code.challenge.mofid.exception.InsufficientFundsException;
import com.code.challenge.mofid.model.AccountBalance;
import com.code.challenge.mofid.model.TransactionRecord;
import com.code.challenge.mofid.model.TransactionType;
import com.code.challenge.mofid.service.AccountRegistry;
import com.code.challenge.mofid.service.BalanceService;
import com.code.challenge.mofid.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@Testcontainers(disabledWithoutDocker = true)
class JdbcReportServiceTest {

    @BeforeEach
    void emptyTables() {
        PostgresBackend.emptyTables();
    }

    @Test
    void reportsBalancesAndOnlyTheTransactionsThatWereApplied() {
        AccountRegistry accounts = PostgresBackend.bean(AccountRegistry.class);
        BalanceService service = PostgresBackend.bean(BalanceService.class);
        ReportService reports = PostgresBackend.bean(ReportService.class);
        accounts.openAccount("A", 1_000);
        accounts.openAccount("B", 500);
        service.credit("A", 100, "TX-1");
        service.credit("A", 100, "TX-1");
        assertThatThrownBy(() -> service.debit("A", 5_000, "TX-2")).isInstanceOf(InsufficientFundsException.class);
        service.transfer("A", "B", 300, "TX-3");

        assertThat(reports.accountBalances())
                .containsExactly(new AccountBalance("A", 800), new AccountBalance("B", 800));
        assertThat(reports.transactions())
                .extracting(TransactionRecord::transactionId, TransactionRecord::type,
                        TransactionRecord::sourceAccountId, TransactionRecord::destinationAccountId, TransactionRecord::amount)
                .containsExactly(
                        tuple("TX-1", TransactionType.CREDIT, null, "A", 100L),
                        tuple("TX-3", TransactionType.TRANSFER, "A", "B", 300L));
        assertThat(reports.transactions()).allSatisfy(report -> assertThat(report.recordedAt()).isNotNull());
    }
}
