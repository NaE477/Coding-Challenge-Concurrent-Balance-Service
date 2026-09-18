package com.code.challenge.mofid.service;

import com.code.challenge.mofid.service.ConcurrencyScenarios.Check;
import com.code.challenge.mofid.service.UnsafeBalanceService.Flaw;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.fail;

/**
 * A test of tests. Checks if our concurrency tests can catch the bugs they're meant to catch in concurrency scenarios
 * (Scenario 1,2,3,6 @ {@link ConcurrencyScenarios}), with balance service that is not concurrency compliant ({@link UnsafeBalanceService}).
 * If a scenario ever stops catching the bug it was written for, this test fails.
 */
class ContractTeethTest {

    private static final int ATTEMPTS = 5;

    @Test
    void mixedLoadScenarioCatchesLostUpdates() {
        assertCatches(Flaw.LOST_UPDATES, ConcurrencyScenarios::mixedCreditsAndDebitsOnOneAccount);
    }

    @Test
    void overdraftScenarioCatchesDoubleSpending() {
        assertCatches(Flaw.LOST_UPDATES, ConcurrencyScenarios::overdraftRaceOnOneAccount);
    }

    @Test
    void twoDebitRaceScenarioCatchesCheckThenAct() {
        assertCatches(Flaw.LOST_UPDATES, scenarios -> scenarios.twoDebitsRaceForTheSameMoney(200));
    }

    @Test
    void duplicateScenarioCatchesNonAtomicDeduplication() {
        assertCatches(Flaw.NON_ATOMIC_DEDUPLICATION, ConcurrencyScenarios::concurrentDuplicatesOfOneTransaction);
    }

    private static void assertCatches(Flaw flaw, Function<ConcurrencyScenarios, List<Check>> scenario) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            UnsafeBalanceService unsafe = new UnsafeBalanceService(flaw);
            if (scenario.apply(new ConcurrencyScenarios(unsafe, unsafe)).stream().anyMatch(check -> !check.holds())) {
                return;
            }
        }
        fail("Scenario passed " + ATTEMPTS + " times against a service with " + flaw);
    }
}
