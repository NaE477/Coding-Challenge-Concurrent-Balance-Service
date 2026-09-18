package com.code.challenge.mofid.service;

import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.InsufficientFundsException;
import com.code.challenge.mofid.support.Concurrently;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrent workloads.
 */
public final class ConcurrencyScenarios {

    public record Check(String what, long expected, long actual) {

        public boolean holds() {
            return expected == actual;
        }
    }

    private final BalanceService service;
    private final AccountRegistry accounts;

    public ConcurrencyScenarios(BalanceService service, AccountRegistry accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    /**
     * Scenario 1: 500 credits of 100 and 500 debits of 70 at once. Any lost update changes the final balance.
     */
    public List<Check> mixedCreditsAndDebitsOnOneAccount() {
        accounts.openAccount("hot", 100_000);
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            String creditId = "credit-" + i;
            String debitId = "debit-" + i;
            tasks.add(() -> service.credit("hot", 100, creditId));
            tasks.add(() -> service.debit("hot", 70, debitId));
        }

        List<Throwable> failures = Concurrently.run(tasks);

        return List.of(
                new Check("operations that failed", 0, countFailures(failures)),
                new Check("final balance", 115_000, service.getBalance("hot")));
    }

    /** Scenario 2: 1,000 debits of 150 at once from 100,000. Exactly 666 fit (666 × 150 = 99,900). */
    public List<Check> overdraftRaceOnOneAccount() {
        accounts.openAccount("hot", 100_000);
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            String debitId = "debit-" + i;
            tasks.add(() -> service.debit("hot", 150, debitId));
        }

        List<Throwable> failures = Concurrently.run(tasks);

        long insufficient = failures.stream().filter(InsufficientFundsException.class::isInstance).count();
        return List.of(
                new Check("debits that succeeded", 666, failures.stream().filter(Objects::isNull).count()),
                new Check("debits rejected for insufficient funds", 334, insufficient),
                new Check("unexpected failures", 0, countFailures(failures) - insufficient),
                new Check("final balance", 100, service.getBalance("hot")));
    }

    /** Scenario 3: two debits of 700 race for the same 1,000, many times over. Exactly one may win each round. */
    public List<Check> twoDebitsRaceForTheSameMoney(int rounds) {
        long exactlyOneSucceeded = 0;
        long endedAt300 = 0;
        for (int round = 0; round < rounds; round++) {
            String account = "race-" + round;
            accounts.openAccount(account, 1_000);
            List<Throwable> failures = Concurrently.run(List.of(
                    () -> service.debit(account, 700, account + "-a"),
                    () -> service.debit(account, 700, account + "-b")));

            if (failures.stream().filter(Objects::isNull).count() == 1) {
                exactlyOneSucceeded++;
            }
            if (service.getBalance(account) == 300) {
                endedAt300++;
            }
        }
        return List.of(
                new Check("rounds where exactly one debit succeeded", rounds, exactlyOneSucceeded),
                new Check("rounds ending at 300", rounds, endedAt300));
    }

    /** Scenario 4: 16 threads × 500 random transfers across 10 accounts. Transfers only move money, so the total can't change. */
    public List<Check> randomTransfersAcrossAccounts() {
        int accountCount = 10;
        for (int i = 0; i < accountCount; i++) {
            accounts.openAccount("acc-" + i, 10_000);
        }
        List<Runnable> tasks = new ArrayList<>();
        for (int t = 0; t < 16; t++) {
            int thread = t;
            tasks.add(() -> {
                Random random = new Random(thread);
                for (int k = 0; k < 500; k++) {
                    int source = random.nextInt(accountCount);
                    int destination = (source + 1 + random.nextInt(accountCount - 1)) % accountCount;
                    try {
                        service.transfer("acc-" + source, "acc-" + destination, 1 + random.nextInt(1_000),
                                "t" + thread + "-" + k);
                    } catch (InsufficientFundsException expected) {
                        // Random amounts can exceed a balance; a rejected transfer must change nothing.
                    }
                }
            });
        }

        List<Throwable> failures = Concurrently.run(tasks);

        long total = 0;
        long belowZero = 0;
        for (int i = 0; i < accountCount; i++) {
            long balance = service.getBalance("acc-" + i);
            total += balance;
            if (balance < 0) {
                belowZero++;
            }
        }
        return List.of(
                new Check("threads that failed", 0, countFailures(failures)),
                new Check("total money", 100_000, total),
                new Check("accounts below zero", 0, belowZero));
    }

    /** Scenario 5: 8 threads move A→B while 8 move B→A. Without a consistent lock order this deadlocks. */
    public List<Check> oppositeTransfersBetweenTwoAccounts() {
        accounts.openAccount("A", 10_000);
        accounts.openAccount("B", 10_000);
        List<Runnable> tasks = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            int thread = t;
            tasks.add(() -> {
                for (int k = 0; k < 250; k++) {
                    service.transfer("A", "B", 1, "ab-" + thread + "-" + k);
                }
            });
            tasks.add(() -> {
                for (int k = 0; k < 250; k++) {
                    service.transfer("B", "A", 1, "ba-" + thread + "-" + k);
                }
            });
        }

        List<Throwable> failures = Concurrently.run(tasks);

        return List.of(
                new Check("threads that failed", 0, countFailures(failures)),
                new Check("balance of A", 10_000, service.getBalance("A")),
                new Check("balance of B", 10_000, service.getBalance("B")));
    }

    /** Scenario 6: 32 threads send the same credit, then the same debit, then the same transfer. Each applies once. */
    public List<Check> concurrentDuplicatesOfOneTransaction() {
        accounts.openAccount("A", 1_000);
        accounts.openAccount("B", 500);
        List<Check> checks = new ArrayList<>();

        long failed = countFailures(Concurrently.run(copies(32, () -> service.credit("A", 100, "dup-credit"))));
        checks.add(new Check("A after 32 duplicate credits of 100", 1_100, service.getBalance("A")));

        failed += countFailures(Concurrently.run(copies(32, () -> service.debit("A", 200, "dup-debit"))));
        checks.add(new Check("A after 32 duplicate debits of 200", 900, service.getBalance("A")));

        failed += countFailures(Concurrently.run(copies(32, () -> service.transfer("A", "B", 300, "dup-transfer"))));
        checks.add(new Check("A after 32 duplicate transfers of 300", 600, service.getBalance("A")));
        checks.add(new Check("B after 32 duplicate transfers of 300", 800, service.getBalance("B")));

        checks.add(new Check("duplicate calls that failed", 0, failed));
        return checks;
    }

    /**
     * Scenario 7: 200 transfers of 1 into an account while it is being deleted.
     */
    public List<Check> transfersRacingADeleteOfTheirDestination() {
        accounts.openAccount("source", 100_000);
        accounts.openAccount("target", 0);
        AtomicBoolean deleteReturned = new AtomicBoolean();
        AtomicInteger succeededAfterDelete = new AtomicInteger();
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            String transactionId = "into-target-" + i;
            tasks.add(() -> {
                boolean startedAfterDelete = deleteReturned.get();
                service.transfer("source", "target", 1, transactionId);
                if (startedAfterDelete) {
                    succeededAfterDelete.incrementAndGet();
                }
            });
        }
        int deleteTask = tasks.size() / 2;
        tasks.add(deleteTask, () -> {
            accounts.deleteAccount("target");
            deleteReturned.set(true);
        });

        List<Throwable> failures = Concurrently.run(tasks);

        List<Throwable> transferFailures = new ArrayList<>(failures);
        Throwable deleteFailure = transferFailures.remove(deleteTask);
        long succeeded = transferFailures.stream().filter(Objects::isNull).count();
        long notFound = transferFailures.stream().filter(AccountNotFoundException.class::isInstance).count();
        long targetGone;
        try {
            service.getBalance("target");
            targetGone = 0;
        } catch (AccountNotFoundException expected) {
            targetGone = 1;
        }
        return List.of(
                new Check("delete failed", 0, deleteFailure == null ? 0 : 1),
                new Check("transfers that started after the delete returned and still succeeded", 0,
                        succeededAfterDelete.get()),
                new Check("transfers that failed for a reason other than the deleted account", 0,
                        transferFailures.size() - succeeded - notFound),
                new Check("source balance (100,000 minus one per successful transfer)", 100_000 - succeeded,
                        service.getBalance("source")),
                new Check("target reported as not found after the delete", 1, targetGone));
    }

    private static List<Runnable> copies(int count, Runnable task) {
        return Collections.nCopies(count, task);
    }

    private static long countFailures(List<Throwable> failures) {
        return failures.stream().filter(Objects::nonNull).count();
    }
}
