package com.code.challenge.mofid.support;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public final class Concurrently {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private Concurrently() {
    }

    /**
     * Runs each task on its own thread, releasing all of them at the same instant, and returns each
     * task's failure ({@code null} if it completed normally), in task order.
     *
     * <p>Fails if the tasks don't all finish in time, for instance, because of a deadlock. The threads are
     * daemons, so a deadlocked run can't keep the JVM alive.
     */
    public static List<Throwable> run(List<? extends Runnable> tasks) {
        int count = tasks.size();
        CyclicBarrier start = new CyclicBarrier(count);
        CountDownLatch finished = new CountDownLatch(count);
        Throwable[] failures = new Throwable[count];

        for (int i = 0; i < count; i++) {
            int index = i;
            Thread.ofPlatform().daemon(true).start(() -> {
                try {
                    start.await();
                    tasks.get(index).run();
                } catch (Throwable failure) {
                    failures[index] = failure;
                } finally {
                    finished.countDown();
                }
            });
        }

        if (!await(finished)) {
            throw new AssertionError(
                    "Tasks did not finish within " + TIMEOUT.toSeconds() + "s - possible deadlock");
        }
        return Arrays.asList(failures);
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for tasks", e);
        }
    }
}
