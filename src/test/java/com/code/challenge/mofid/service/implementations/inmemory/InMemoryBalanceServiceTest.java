package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.InsufficientFundsException;
import com.code.challenge.mofid.service.BalanceService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test of the facade using the challenge's own examples.
 */
class InMemoryBalanceServiceTest {

    private final InMemoryFixture fixture = new InMemoryFixture();
    private final BalanceService service = fixture.balanceService;

    @Test
    void credit() {
        fixture.accounts.openAccount("A", 1_000);

        service.credit("A", 500, "TX-1");

        assertThat(service.getBalance("A")).isEqualTo(1_500);
    }

    @Test
    void debit() {
        fixture.accounts.openAccount("A", 1_000);

        service.debit("A", 700, "TX-1");

        assertThat(service.getBalance("A")).isEqualTo(300);
    }

    @Test
    void debitBeyondTheBalanceFailsAndChangesNothing() {
        fixture.accounts.openAccount("A", 1_000);

        assertThatThrownBy(() -> service.debit("A", 1_200, "TX-1"))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(service.getBalance("A")).isEqualTo(1_000);
    }

    @Test
    void transfer() {
        fixture.accounts.openAccount("A", 1_000);
        fixture.accounts.openAccount("B", 500);

        service.transfer("A", "B", 300, "TX-1");

        assertThat(service.getBalance("A")).isEqualTo(700);
        assertThat(service.getBalance("B")).isEqualTo(800);
    }

    @Test
    void repeatedCreditIsAppliedOnce() {
        fixture.accounts.openAccount("A", 1_000);

        service.credit("A", 100, "TX-1");
        service.credit("A", 100, "TX-1");
        service.credit("A", 100, "TX-1");

        assertThat(service.getBalance("A")).isEqualTo(1_100);
    }

    @Test
    void unknownAccountIsRejected() {
        assertThatThrownBy(() -> service.getBalance("missing")).isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> service.credit("missing", 100, "TX-1")).isInstanceOf(AccountNotFoundException.class);
    }
}
