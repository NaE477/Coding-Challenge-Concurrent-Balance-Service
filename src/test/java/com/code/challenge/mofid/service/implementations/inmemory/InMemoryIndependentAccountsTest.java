package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.service.implementations.inmemory.model.InMemoryLockedAccount;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locking is per account: while account A's lock is held, work on B proceeds and only work on A waits.
 */
class InMemoryIndependentAccountsTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final InMemoryFixture fixture = new InMemoryFixture();

    @Test
    void operationsOnOtherAccountsAreNotBlockedByAHeldAccount() throws InterruptedException {
        fixture.accounts.openAccount("A", 1_000);
        fixture.accounts.openAccount("B", 1_000);
        InMemoryLockedAccount accountA = fixture.accounts.get("A");

        accountA.lock().lock();
        Thread creditOnA;
        try {
            Thread creditOnB = Thread.ofPlatform().daemon(true)
                    .start(() -> fixture.creditService.credit("B", 100, "TX-B"));
            creditOnB.join(TIMEOUT);
            assertThat(creditOnB.isAlive()).as("credit on B must not wait for A's lock").isFalse();

            creditOnA = Thread.ofPlatform().daemon(true)
                    .start(() -> fixture.creditService.credit("A", 100, "TX-A"));
            awaitParked(creditOnA);
            assertThat(creditOnA.getState()).as("credit on A must wait for A's lock").isEqualTo(Thread.State.WAITING);
        } finally {
            accountA.lock().unlock();
        }

        creditOnA.join(TIMEOUT);
        assertThat(creditOnA.isAlive()).as("credit on A must finish once the lock is released").isFalse();
        assertThat(fixture.balanceQueryService.getBalance("A")).isEqualTo(1_100);
        assertThat(fixture.balanceQueryService.getBalance("B")).isEqualTo(1_100);
    }

    private static void awaitParked(Thread thread) {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (thread.getState() != Thread.State.WAITING
                && thread.getState() != Thread.State.TERMINATED
                && Instant.now().isBefore(deadline)) {
            Thread.onSpinWait();
        }
    }
}
