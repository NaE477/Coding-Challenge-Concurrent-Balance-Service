package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.exception.IdempotencyConflictException;
import com.code.challenge.mofid.model.TransactionRequest;
import com.code.challenge.mofid.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryTransactionLedgerTest {

    private static final TransactionRequest CREDIT_A = new TransactionRequest(TransactionType.CREDIT, null, "A", 100);
    private static final TransactionRequest DEBIT_A = new TransactionRequest(TransactionType.DEBIT, "A", null, 100);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final InMemoryTransactionLedger ledger = new InMemoryTransactionLedger();
    private final AtomicInteger executions = new AtomicInteger();

    @Test
    void duplicateWithTheSameRequestRunsTheOperationOnce() {
        ledger.executeOperation("TX-1", CREDIT_A, executions::incrementAndGet);
        ledger.executeOperation("TX-1", CREDIT_A, executions::incrementAndGet);
        ledger.executeOperation("TX-1", CREDIT_A, executions::incrementAndGet);

        assertThat(executions).hasValue(1);
    }

    @Test
    void sameIdWithADifferentRequestIsAConflict() {
        ledger.executeOperation("TX-1", CREDIT_A, executions::incrementAndGet);

        assertThatThrownBy(() -> ledger.executeOperation("TX-1", DEBIT_A, executions::incrementAndGet))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasFieldOrPropertyWithValue("transactionId", "TX-1");
        assertThat(executions).hasValue(1);
    }

    @Test
    void failedOperationDoesNotConsumeTheTransactionId() {
        assertThatThrownBy(() -> ledger.executeOperation("TX-1", CREDIT_A, () -> {
            throw new IllegalStateException("boom");
        })).hasMessage("boom");

        ledger.executeOperation("TX-1", CREDIT_A, executions::incrementAndGet);

        assertThat(executions).hasValue(1);
    }

    @Test
    void failedOperationCanBeRetriedWithADifferentRequest() {
        assertThatThrownBy(() -> ledger.executeOperation("TX-1", CREDIT_A, () -> {
            throw new IllegalStateException("boom");
        })).hasMessage("boom");

        ledger.executeOperation("TX-1", DEBIT_A, executions::incrementAndGet);

        assertThat(executions).hasValue(1);
    }

    @Test
    void concurrentDuplicatesRunOnceAndNoneReturnsBeforeTheOperationCompletes() throws Exception {
        int threads = 32;
        CyclicBarrier start = new CyclicBarrier(threads);
        CountDownLatch operationStarted = new CountDownLatch(1);
        CountDownLatch releaseOperation = new CountDownLatch(1);
        AtomicBoolean operationCompleted = new AtomicBoolean();
        AtomicInteger returnedEarly = new AtomicInteger();
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        Runnable operation = () -> {
            executions.incrementAndGet();
            operationStarted.countDown();
            awaitQuietly(releaseOperation);
            operationCompleted.set(true);
        };

        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            workers.add(Thread.ofPlatform().start(() -> {
                try {
                    start.await();
                    ledger.executeOperation("TX-1", CREDIT_A, operation);
                    if (!operationCompleted.get()) {
                        returnedEarly.incrementAndGet();
                    }
                } catch (Throwable t) {
                    failures.add(t);
                }
            }));
        }

        assertThat(operationStarted.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();
        awaitAllWaiting(workers);
        releaseOperation.countDown();
        joinAll(workers);

        assertThat(failures).isEmpty();
        assertThat(executions).hasValue(1);
        assertThat(returnedEarly).hasValue(0);
    }

    @Test
    void waitingDuplicateReceivesTheFailureOfTheOperationItWaitedFor() throws Exception {
        CountDownLatch operationStarted = new CountDownLatch(1);
        CountDownLatch releaseOperation = new CountDownLatch(1);
        IllegalStateException boom = new IllegalStateException("boom");
        Queue<Throwable> thrown = new ConcurrentLinkedQueue<>();

        Runnable failingOperation = () -> {
            executions.incrementAndGet();
            operationStarted.countDown();
            awaitQuietly(releaseOperation);
            throw boom;
        };
        Runnable call = () -> {
            try {
                ledger.executeOperation("TX-1", CREDIT_A, failingOperation);
            } catch (Throwable t) {
                thrown.add(t);
            }
        };

        Thread winner = Thread.ofPlatform().start(call);
        assertThat(operationStarted.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();
        Thread duplicate = Thread.ofPlatform().start(call);
        awaitAllWaiting(List.of(winner, duplicate));
        releaseOperation.countDown();
        joinAll(List.of(winner, duplicate));

        assertThat(executions).as("the duplicate must wait, not run the operation again").hasValue(1);
        assertThat(thrown).containsExactly(boom, boom);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            if (!latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                throw new IllegalStateException("latch was never released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void awaitAllWaiting(List<Thread> threads) {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (!threads.stream().allMatch(t -> t.getState() == Thread.State.WAITING
                || t.getState() == Thread.State.TIMED_WAITING)) {
            if (threads.stream().anyMatch(t -> t.getState() == Thread.State.TERMINATED)
                    || Instant.now().isAfter(deadline)) {
                return;
            }
            Thread.onSpinWait();
        }
    }

    private static void joinAll(List<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join(TIMEOUT);
            assertThat(thread.isAlive()).as("thread %s did not finish", thread.getName()).isFalse();
        }
    }
}
