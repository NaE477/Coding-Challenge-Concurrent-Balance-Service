package com.code.challenge.mofid.service.implementations.inmemory.model;

import com.code.challenge.mofid.exception.BalanceOverflowException;
import com.code.challenge.mofid.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryLockedAccountTest {

    @Test
    void everyAccessorRequiresTheLock() {
        InMemoryLockedAccount account = new InMemoryLockedAccount("A", 1_000);

        assertThatThrownBy(account::balance).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> account.deposit(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> account.withdraw(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> account.requireFunds(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> account.requireRoomFor(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void depositAndWithdrawChangeTheBalance() {
        InMemoryLockedAccount account = new InMemoryLockedAccount("A", 1_000);

        locked(account, a -> a.deposit(500));
        locked(account, a -> a.withdraw(700));

        assertThat(balanceOf(account)).isEqualTo(800);
    }

    @Test
    void withdrawingTheWholeBalanceLeavesZero() {
        InMemoryLockedAccount account = new InMemoryLockedAccount("A", 1_000);

        locked(account, a -> a.withdraw(1_000));

        assertThat(balanceOf(account)).isZero();
    }

    @Test
    void insufficientFundsLeavesTheBalanceUnchanged() {
        InMemoryLockedAccount account = new InMemoryLockedAccount("A", 1_000);

        assertThatThrownBy(() -> locked(account, a -> a.withdraw(1_200)))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(balanceOf(account)).isEqualTo(1_000);
    }

    @Test
    void overflowLeavesTheBalanceUnchanged() {
        InMemoryLockedAccount account = new InMemoryLockedAccount("A", Long.MAX_VALUE - 10);

        assertThatThrownBy(() -> locked(account, a -> a.deposit(11)))
                .isInstanceOf(BalanceOverflowException.class);
        assertThat(balanceOf(account)).isEqualTo(Long.MAX_VALUE - 10);
    }

    @Test
    void depositUpToExactlyTheMaximumIsAllowed() {
        InMemoryLockedAccount account = new InMemoryLockedAccount("A", Long.MAX_VALUE - 10);

        locked(account, a -> a.deposit(10));

        assertThat(balanceOf(account)).isEqualTo(Long.MAX_VALUE);
    }

    private static void locked(InMemoryLockedAccount account, Consumer<InMemoryLockedAccount> action) {
        account.lock().lock();
        try {
            action.accept(account);
        } finally {
            account.lock().unlock();
        }
    }

    private static long balanceOf(InMemoryLockedAccount account) {
        account.lock().lock();
        try {
            return account.balance();
        } finally {
            account.lock().unlock();
        }
    }
}
