package Task04;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScannerExperimentsDemo {

    // Helper to run 4 stations and record finishing order
    public static List<Integer> runStationsWithOrder(int[] scriptCounts, long[] delays, int[] priorities) throws InterruptedException {
        final List<Integer> finishOrder = new CopyOnWriteArrayList<>();
        Thread[] stations = new Thread[4];

        for (int i = 0; i < 4; i++) {
            final int id = i + 1;
            final int count = scriptCounts[i];
            final long delay = delays[i];
            stations[i] = new Thread(() -> {
                try {
                    for (int s = 0; s < count; s++) {
                        Thread.sleep(delay);
                    }
                    finishOrder.add(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Station-" + id);

            if (priorities != null) {
                stations[i].setPriority(priorities[i]);
            }
        }

        for (Thread t : stations) t.start();
        for (Thread t : stations) t.join();

        return finishOrder;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("    TASK 04: THREAD CREATION & THREAD LIFE CYCLE - PARALLEL SCANNER STATIONS");
        System.out.println("================================================================================");

        // -------------------------------------------------------------------------
        // SECTION 1: Demonstrating Thread Extension with start(), sleep(), join(), isAlive()
        // -------------------------------------------------------------------------
        System.out.println("\n[SECTION 1: Extending Thread Class & Life-Cycle Methods]");
        AnswerScriptScannerThread[] threadStations = new AnswerScriptScannerThread[4];
        for (int i = 0; i < 4; i++) {
            threadStations[i] = new AnswerScriptScannerThread(i + 1, 10, 15);
            System.out.printf("Created Station-%d | Initial Thread State: %s | isAlive: %b%n",
                    i + 1, threadStations[i].getState(), threadStations[i].isAlive());
        }

        // Start all threads
        for (AnswerScriptScannerThread t : threadStations) {
            t.start();
        }

        // Check isAlive immediately after starting
        System.out.printf("After start() | Station-1 State: %s | isAlive: %b%n",
                threadStations[0].getState(), threadStations[0].isAlive());

        // Wait for all to complete using join()
        int totalScannedThread = 0;
        for (AnswerScriptScannerThread t : threadStations) {
            t.join();
            totalScannedThread += t.getScannedCount();
        }

        System.out.printf("After join() | Station-1 State: %s | isAlive: %b%n",
                threadStations[0].getState(), threadStations[0].isAlive());
        System.out.println("ALL STATIONS COMPLETED. Total Scripts Scanned (Thread): " + totalScannedThread);

        // -------------------------------------------------------------------------
        // SECTION 2: Demonstrating Runnable Implementation
        // -------------------------------------------------------------------------
        System.out.println("\n[SECTION 2: Implementing Runnable Interface]");
        AnswerScriptScannerRunnable[] runnables = new AnswerScriptScannerRunnable[4];
        Thread[] runnableWorkers = new Thread[4];

        for (int i = 0; i < 4; i++) {
            runnables[i] = new AnswerScriptScannerRunnable(i + 1, 10, 15);
            runnableWorkers[i] = new Thread(runnables[i], "RunnableWorker-" + (i + 1));
            runnableWorkers[i].start();
        }

        int totalScannedRunnable = 0;
        for (int i = 0; i < 4; i++) {
            runnableWorkers[i].join();
            totalScannedRunnable += runnables[i].getScannedCount();
        }
        System.out.println("ALL RUNNABLE STATIONS COMPLETED. Total Scripts Scanned (Runnable): " + totalScannedRunnable);

        // -------------------------------------------------------------------------
        // SECTION 3: Evidence of run() vs start()
        // -------------------------------------------------------------------------
        System.out.println("\n[SECTION 3: Student A - Proof that calling run() does not create a new Thread]");
        AnswerScriptScannerThread directRunThread = new AnswerScriptScannerThread(99, 2, 5);
        System.out.println("Current thread before calling run(): " + Thread.currentThread().getName());
        System.out.println("Executing directRunThread.run():");
        directRunThread.run(); // Synchronous call on caller thread!
        System.out.println("Notice: It ran on '" + Thread.currentThread().getName() + "' thread, NOT a new background thread!");

        // -------------------------------------------------------------------------
        // SECTION 4: Student B - 10 Runs Recording Completion Order
        // -------------------------------------------------------------------------
        System.out.println("\n[SECTION 4: Student B - 10 Executions to Record Non-Deterministic Order]");
        int[] uniformCounts = {10, 10, 10, 10};
        long[] uniformDelays = {5, 5, 5, 5};

        System.out.println("Run #   | Finish Order (Stations)");
        System.out.println("--------|------------------------");
        for (int run = 1; run <= 10; run++) {
            List<Integer> order = runStationsWithOrder(uniformCounts, uniformDelays, null);
            System.out.printf("Run %-3d | %s%n", run, order);
        }

        // -------------------------------------------------------------------------
        // SECTION 5: Student B - Thread Priority Experiment
        // -------------------------------------------------------------------------
        System.out.println("\n[SECTION 5: Student B - Thread Priority Experiment]");
        int[] priorities = {Thread.MIN_PRIORITY, Thread.NORM_PRIORITY, Thread.NORM_PRIORITY, Thread.MAX_PRIORITY};
        System.out.println("Testing Priorities: Station-1 (MIN=1), Station-2 (NORM=5), Station-3 (NORM=5), Station-4 (MAX=10)");
        for (int run = 1; run <= 5; run++) {
            List<Integer> prioOrder = runStationsWithOrder(uniformCounts, uniformDelays, priorities);
            System.out.printf("Priority Run %d | Finish Order: %s%n", run, prioOrder);
        }
        System.out.println("Observation: Thread priorities are merely hints to the OS scheduler and do NOT guarantee execution order.");

        // -------------------------------------------------------------------------
        // SECTION 6: Student B - Daemon Thread Experiment
        // -------------------------------------------------------------------------
        System.out.println("\n[SECTION 6: Student B - Daemon Thread Experiment]");
        Thread daemonStation = new Thread(() -> {
            try {
                System.out.println("Daemon Station started scanning long batch...");
                for (int i = 1; i <= 100; i++) {
                    Thread.sleep(100);
                }
                System.out.println("Daemon Station finished completely (NEVER REACHED if main exits).");
            } catch (InterruptedException e) {
                System.out.println("Daemon Station interrupted.");
            }
        }, "DaemonStation-4");

        daemonStation.setDaemon(true);
        System.out.println("Is daemonStation daemon? -> " + daemonStation.isDaemon());
        daemonStation.start();
        Thread.sleep(20);
        System.out.println("Main method ends now. Because daemonStation is a daemon, JVM does not wait for it to finish!");
    }
}
