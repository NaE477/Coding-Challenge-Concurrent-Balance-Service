package com.code.challenge.mofid.service.implementations.inmemory;

import com.code.challenge.mofid.exception.AccountNotFoundException;
import com.code.challenge.mofid.exception.BalanceOverflowException;
import com.code.challenge.mofid.exception.InsufficientFundsException;
import com.code.challenge.mofid.service.TransferService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryTransferServiceTest {

    private final InMemoryFixture fixture = new InMemoryFixture();
    private final TransferService transfers = fixture.transferService;

    @Test
    void movesTheExactAmountBetweenAccounts() {
        fixture.accounts.openAccount("A", 1_000);
        fixture.accounts.openAccount("B", 500);

        transfers.transfer("A", "B", 300, "TX-1");

        assertThat(balance("A")).isEqualTo(700);
        assertThat(balance("B")).isEqualTo(800);
    }

    @Test
    void worksInBothLockOrders() {
        fixture.accounts.openAccount("A", 1_000);
        fixture.accounts.openAccount("B", 500);

        transfers.transfer("B", "A", 200, "TX-1");

        assertThat(balance("A")).isEqualTo(1_200);
        assertThat(balance("B")).isEqualTo(300);
    }

    @Test
    void insufficientFundsLeavesBothBalancesUnchanged() {
        fixture.accounts.openAccount("A", 100);
        fixture.accounts.openAccount("B", 500);

        assertThatThrownBy(() -> transfers.transfer("A", "B", 300, "TX-1"))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(balance("A")).isEqualTo(100);
        assertThat(balance("B")).isEqualTo(500);
    }

    @Test
    void destinationOverflowLeavesBothBalancesUnchanged() {
        fixture.accounts.openAccount("A", 1_000);
        fixture.accounts.openAccount("B", Long.MAX_VALUE - 10);

        assertThatThrownBy(() -> transfers.transfer("A", "B", 300, "TX-1"))
                .isInstanceOf(BalanceOverflowException.class);

        assertThat(balance("A")).as("source must not be debited").isEqualTo(1_000);
        assertThat(balance("B")).isEqualTo(Long.MAX_VALUE - 10);
    }

    @Test
    void unknownDestinationLeavesTheSourceUnchanged() {
        fixture.accounts.openAccount("A", 1_000);

        assertThatThrownBy(() -> transfers.transfer("A", "missing", 300, "TX-1"))
                .isInstanceOf(AccountNotFoundException.class);

        assertThat(balance("A")).isEqualTo(1_000);
    }

    @Test
    void failedTransferCanBeRetriedWithTheSameTransactionId() {
        fixture.accounts.openAccount("A", 100);
        fixture.accounts.openAccount("B", 0);
        assertThatThrownBy(() -> transfers.transfer("A", "B", 300, "TX-1"))
                .isInstanceOf(InsufficientFundsException.class);

        fixture.creditService.credit("A", 500, "TX-2");
        transfers.transfer("A", "B", 300, "TX-1");

        assertThat(balance("A")).isEqualTo(300);
        assertThat(balance("B")).isEqualTo(300);
    }

    private long balance(String accountId) {
        return fixture.balanceQueryService.getBalance(accountId);
    }
}
