package com.code.challenge.mofid.service;

import com.code.challenge.mofid.exception.*;
import com.code.challenge.mofid.service.ConcurrencyScenarios.Check;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A backend implementation test binds to it by extending this class and implementing {@link #createBackend()}.
 */
public abstract class BalanceServiceContractTest {

    public record Backend(BalanceService service, AccountRegistry accounts) {
    }

    /** A fresh, empty backend; called before every test. */
    protected abstract Backend createBackend();

    /** Rounds of the two-debit race (Scecnario 3). A slower backend may lower it. */
    protected int raceRounds() {
        return 1_000;
    }

    protected BalanceService service;
    protected AccountRegistry accounts;

    @BeforeEach
    protected void startWithFreshBackend() {
        Backend backend = createBackend();
        service = backend.service();
        accounts = backend.accounts();
    }

    @Nested
    class Validation {

        @ParameterizedTest
        @ValueSource(longs = {0, -1})
        void nonPositiveAmountIsRejectedAndChangesNothing(long amount) {
            accounts.openAccount("A", 1_000);
            accounts.openAccount("B", 500);

            assertThatThrownBy(() -> service.credit("A", amount, "TX-1")).isInstanceOf(InvalidAmountException.class);
            assertThatThrownBy(() -> service.debit("A", amount, "TX-2")).isInstanceOf(InvalidAmountException.class);
            assertThatThrownBy(() -> service.transfer("A", "B", amount, "TX-3"))
                    .isInstanceOf(InvalidAmountException.class);

            assertBalances(1_000, 500);
        }

        @Test
        void blankIdsAreRejected() {
            accounts.openAccount("A", 1_000);

            assertThatThrownBy(() -> service.credit(" ", 100, "TX-1")).isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> service.credit("A", 100, "")).isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> service.getBalance(null)).isInstanceOf(InvalidRequestException.class);
            assertThat(service.getBalance("A")).isEqualTo(1_000);
        }

        @Test
        void unknownAccountIsRejectedByEveryOperation() {
            accounts.openAccount("A", 1_000);

            assertThatThrownBy(() -> service.credit("missing", 100, "TX-1")).isInstanceOf(AccountNotFoundException.class);
            assertThatThrownBy(() -> service.debit("missing", 100, "TX-2")).isInstanceOf(AccountNotFoundException.class);
            assertThatThrownBy(() -> service.transfer("A", "missing", 100, "TX-3"))
                    .isInstanceOf(AccountNotFoundException.class);
            assertThatThrownBy(() -> service.transfer("missing", "A", 100, "TX-4"))
                    .isInstanceOf(AccountNotFoundException.class);
            assertThatThrownBy(() -> service.getBalance("missing")).isInstanceOf(AccountNotFoundException.class);

            assertThat(service.getBalance("A")).isEqualTo(1_000);
        }

        @Test
        void transferToTheSameAccountIsRejected() {
            accounts.openAccount("A", 1_000);

            assertThatThrownBy(() -> service.transfer("A", "A", 100, "TX-1"))
                    .isInstanceOf(SameAccountTransferException.class);

            assertThat(service.getBalance("A")).isEqualTo(1_000);
        }

        @Test
        void rejectedRequestDoesNotUseUpItsTransactionId() {
            accounts.openAccount("A", 1_000);
            assertThatThrownBy(() -> service.credit("A", 0, "TX-1")).isInstanceOf(InvalidAmountException.class);

            service.credit("A", 100, "TX-1");

            assertThat(service.getBalance("A")).isEqualTo(1_100);
        }
    }

    @Nested
    class Behaviour {

        @Test
        void credit() {
            accounts.openAccount("A", 1_000);

            service.credit("A", 500, "TX-1");

            assertThat(service.getBalance("A")).isEqualTo(1_500);
        }

        @Test
        void debit() {
            accounts.openAccount("A", 1_000);

            service.debit("A", 700, "TX-1");

            assertThat(service.getBalance("A")).isEqualTo(300);
        }

        @Test
        void debitOfTheWholeBalanceLeavesZero() {
            accounts.openAccount("A", 1_000);

            service.debit("A", 1_000, "TX-1");

            assertThat(service.getBalance("A")).isZero();
        }

        @Test
        void debitBeyondTheBalanceFailsAndChangesNothing() {
            accounts.openAccount("A", 1_000);

            assertThatThrownBy(() -> service.debit("A", 1_200, "TX-1")).isInstanceOf(InsufficientFundsException.class);

            assertThat(service.getBalance("A")).isEqualTo(1_000);
        }

        @Test
        void creditThatWouldOverflowFailsAndChangesNothing() {
            accounts.openAccount("A", Long.MAX_VALUE - 10);

            assertThatThrownBy(() -> service.credit("A", 11, "TX-1")).isInstanceOf(BalanceOverflowException.class);

            assertThat(service.getBalance("A")).isEqualTo(Long.MAX_VALUE - 10);
        }

        @Test
        void transferMovesTheExactAmount() {
            accounts.openAccount("A", 1_000);
            accounts.openAccount("B", 500);

            service.transfer("A", "B", 300, "TX-1");

            assertBalances(700, 800);
        }

        @Test
        void transferWorksInBothDirections() {
            accounts.openAccount("A", 1_000);
            accounts.openAccount("B", 500);

            service.transfer("B", "A", 200, "TX-1");

            assertBalances(1_200, 300);
        }

        @Test
        void transferWithInsufficientFundsChangesNeitherAccount() {
            accounts.openAccount("A", 100);
            accounts.openAccount("B", 500);

            assertThatThrownBy(() -> service.transfer("A", "B", 300, "TX-1"))
                    .isInstanceOf(InsufficientFundsException.class);

            assertBalances(100, 500);
        }

        @Test
        void transferThatWouldOverflowTheDestinationChangesNeitherAccount() {
            accounts.openAccount("A", 1_000);
            accounts.openAccount("B", Long.MAX_VALUE - 10);

            assertThatThrownBy(() -> service.transfer("A", "B", 300, "TX-1"))
                    .isInstanceOf(BalanceOverflowException.class);

            assertBalances(1_000, Long.MAX_VALUE - 10);
        }
    }

    @Nested
    class Idempotency {

        @Test
        void repeatedCreditIsAppliedOnce() {
            accounts.openAccount("A", 1_000);

            service.credit("A", 100, "TX-1");
            service.credit("A", 100, "TX-1");
            service.credit("A", 100, "TX-1");

            assertThat(service.getBalance("A")).isEqualTo(1_100);
        }

        @Test
        void repeatedDebitIsAppliedOnce() {
            accounts.openAccount("A", 1_000);

            service.debit("A", 100, "TX-1");
            service.debit("A", 100, "TX-1");
            service.debit("A", 100, "TX-1");

            assertThat(service.getBalance("A")).isEqualTo(900);
        }

        @Test
        void repeatedTransferIsAppliedOnce() {
            accounts.openAccount("A", 1_000);
            accounts.openAccount("B", 500);

            service.transfer("A", "B", 300, "TX-1");
            service.transfer("A", "B", 300, "TX-1");
            service.transfer("A", "B", 300, "TX-1");

            assertBalances(700, 800);
        }

        @Test
        void sameIdWithADifferentRequestIsAConflictAndChangesNothing() {
            accounts.openAccount("A", 1_000);
            accounts.openAccount("B", 500);
            service.credit("A", 100, "TX-1");

            assertThatThrownBy(() -> service.credit("A", 200, "TX-1"))
                    .isInstanceOf(IdempotencyConflictException.class);
            assertThatThrownBy(() -> service.debit("A", 100, "TX-1"))
                    .isInstanceOf(IdempotencyConflictException.class);
            assertThatThrownBy(() -> service.transfer("A", "B", 100, "TX-1"))
                    .isInstanceOf(IdempotencyConflictException.class);

            assertBalances(1_100, 500);
        }

        @Test
        void failedDebitCanBeRetriedWithTheSameId() {
            accounts.openAccount("A", 1_000);
            assertThatThrownBy(() -> service.debit("A", 1_500, "TX-1")).isInstanceOf(InsufficientFundsException.class);

            service.credit("A", 1_000, "TX-2");
            service.debit("A", 1_500, "TX-1");

            assertThat(service.getBalance("A")).isEqualTo(500);
        }

        @Test
        void failedTransferCanBeRetriedWithTheSameId() {
            accounts.openAccount("A", 100);
            accounts.openAccount("B", 0);
            assertThatThrownBy(() -> service.transfer("A", "B", 300, "TX-1"))
                    .isInstanceOf(InsufficientFundsException.class);

            service.credit("A", 500, "TX-2");
            service.transfer("A", "B", 300, "TX-1");

            assertBalances(300, 300);
        }
    }

    @Nested
    class Concurrency {

        @Test
        void mixedCreditsAndDebitsOnOneAccountLoseNoUpdates() {
            assertAllHold(scenarios().mixedCreditsAndDebitsOnOneAccount());
        }

        @Test
        void concurrentDebitsNeverOverdraw() {
            assertAllHold(scenarios().overdraftRaceOnOneAccount());
        }

        @Test
        void exactlyOneOfTwoRacingDebitsWinsEveryRound() {
            assertAllHold(scenarios().twoDebitsRaceForTheSameMoney(raceRounds()));
        }

        @Test
        void randomTransfersAcrossAccountsConserveTheTotal() {
            assertAllHold(scenarios().randomTransfersAcrossAccounts());
        }

        @Test
        void oppositeTransfersDoNotDeadlock() {
            assertAllHold(scenarios().oppositeTransfersBetweenTwoAccounts());
        }

        @Test
        void concurrentDuplicatesAreAppliedOnce() {
            assertAllHold(scenarios().concurrentDuplicatesOfOneTransaction());
        }

        private ConcurrencyScenarios scenarios() {
            return new ConcurrencyScenarios(service, accounts);
        }

        private static void assertAllHold(List<Check> checks) {
            assertThat(checks).isNotEmpty().allSatisfy(check ->
                    assertThat(check.actual()).as(check.what()).isEqualTo(check.expected()));
        }
    }

    private void assertBalances(long expectedA, long expectedB) {
        assertThat(service.getBalance("A")).as("balance of A").isEqualTo(expectedA);
        assertThat(service.getBalance("B")).as("balance of B").isEqualTo(expectedB);
    }
}
