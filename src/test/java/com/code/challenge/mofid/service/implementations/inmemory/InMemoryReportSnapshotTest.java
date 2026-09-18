package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.exception.InsufficientFundsException;
import com.code.challenge.mofid.model.AccountBalance;
import com.code.challenge.mofid.support.Concurrently;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryReportSnapshotTest {

    private static final int ACCOUNTS = 10;
    private static final long TOTAL = ACCOUNTS * 10_000L;

    @Test
    void everyReportTakenDuringConcurrentTransfersShowsTheConservedTotal() {
        InMemoryFixture fixture = new InMemoryFixture();
        InMemoryReportService reports = new InMemoryReportService(fixture.accounts, fixture.ledger);
        for (int i = 0; i < ACCOUNTS; i++) {
            fixture.accounts.openAccount("acc-" + i, 10_000);
        }
        AtomicInteger reportsTaken = new AtomicInteger();
        AtomicInteger inconsistentReports = new AtomicInteger();

        List<Runnable> tasks = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            int thread = t;
            tasks.add(() -> {
                Random random = new Random(thread);
                for (int k = 0; k < 500; k++) {
                    int source = random.nextInt(ACCOUNTS);
                    int destination = (source + 1 + random.nextInt(ACCOUNTS - 1)) % ACCOUNTS;
                    try {
                        fixture.transferService.transfer("acc-" + source, "acc-" + destination,
                                1 + random.nextInt(1_000), "t" + thread + "-" + k);
                    } catch (InsufficientFundsException expected) {
                        // A rejected transfer changes nothing, so it can't affect the total.
                    }
                }
            });
        }
        for (int r = 0; r < 2; r++) {
            tasks.add(() -> {
                for (int i = 0; i < 200; i++) {
                    long total = reports.accountBalances().stream().mapToLong(AccountBalance::balance).sum();
                    reportsTaken.incrementAndGet();
                    if (total != TOTAL) {
                        inconsistentReports.incrementAndGet();
                    }
                }
            });
        }

        List<Throwable> failures = Concurrently.run(tasks);

        assertThat(failures).isNotEmpty().allMatch(Objects::isNull);
        assertThat(reportsTaken).hasValue(400);
        assertThat(inconsistentReports).as("reports whose total differed from %d", TOTAL).hasValue(0);
    }
}
