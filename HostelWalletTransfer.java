package Task06;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HostelWalletTransfer {

    // -------------------------------------------------------------
    // Part A: Hostel Wallet Account
    // -------------------------------------------------------------
    static class Account {
        final int id;
        double balance;

        Account(int id, double balance) {
            this.id = id;
            this.balance = balance;
        }

        // Deadlock-prone transfer: Locks 'from' then 'to' naively
        public static boolean transferVulnerable(Account from, Account to, double amount) {
            synchronized (from) {
                try {
                    // Small sleep makes concurrent interleaving reliably produce circular wait
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}

                synchronized (to) {
                    if (from.balance >= amount) {
                        from.balance -= amount;
                        to.balance += amount;
                        return true;
                    }
                    return false;
                }
            }
        }

        // Deadlock-free transfer: Enforces strict consistent lock ordering based on account ID
        public static boolean transferSafe(Account from, Account to, double amount) {
            Account firstLock = from.id < to.id ? from : to;
            Account secondLock = from.id < to.id ? to : from;

            synchronized (firstLock) {
                synchronized (secondLock) {
                    if (from.balance >= amount) {
                        from.balance -= amount;
                        to.balance += amount;
                        return true;
                    }
                    return false;
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Part B: Monitored Buffer tracking size invariants
    // -------------------------------------------------------------
    static class MonitoredTokenCounter {
        private final int capacity = 5;
        private int count = 0;
        private final List<Integer> history = Collections.synchronizedList(new ArrayList<>());
        private boolean violationDetected = false;

        public synchronized void put(int token) throws InterruptedException {
            while (count == capacity) {
                wait();
            }
            count++;
            history.add(count);
            if (count > capacity || count < 0) violationDetected = true;
            notifyAll();
        }

        public synchronized int take() throws InterruptedException {
            while (count == 0) {
                wait();
            }
            int val = count;
            count--;
            history.add(count);
            if (count > capacity || count < 0) violationDetected = true;
            notifyAll();
            return val;
        }

        public boolean isViolationDetected() { return violationDetected; }
        public List<Integer> getHistory() { return history; }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("   TASK 06: DEADLOCK & INTER-THREAD COMMUNICATION");
        System.out.println("================================================================================");

        // -------------------------------------------------------------
        // SECTION 1: Deadlock Demonstration (Part A)
        // -------------------------------------------------------------
        System.out.println("\n[SECTION 1: Demonstrating Deadlock via Circular Wait]");
        Account accA = new Account(101, 1000.0);
        Account accB = new Account(102, 1000.0);

        Thread t1 = new Thread(() -> Account.transferVulnerable(accA, accB, 100.0), "TransferThread-1");
        Thread t2 = new Thread(() -> Account.transferVulnerable(accB, accA, 150.0), "TransferThread-2");
        t1.setDaemon(true);
        t2.setDaemon(true);

        t1.start();
        t2.start();

        // Check for deadlock after short wait using JVM ThreadMXBean
        Thread.sleep(300);
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreadIds = mxBean.findDeadlockedThreads();

        if (deadlockedThreadIds != null && deadlockedThreadIds.length > 0) {
            System.out.println("DETECTED DEADLOCK! Number of deadlocked threads: " + deadlockedThreadIds.length);
            for (long id : deadlockedThreadIds) {
                System.out.println(" -> Deadlocked Thread: " + mxBean.getThreadInfo(id).getThreadName()
                        + " waiting on lock held by another thread!");
            }
            System.out.println("Coffman Conditions Present: Mutual Exclusion, Hold & Wait, No Preemption, Circular Wait.");
        } else {
            System.out.println("No deadlock detected in this run window.");
        }

        // -------------------------------------------------------------
        // SECTION 2: Deadlock Prevention via Global Lock Ordering (Part A)
        // -------------------------------------------------------------
        System.out.println("\n[SECTION 2: Deadlock Prevention via Consistent Lock Ordering]");
        Account accSafe1 = new Account(201, 1000.0);
        Account accSafe2 = new Account(202, 1000.0);

        Thread safeT1 = new Thread(() -> Account.transferSafe(accSafe1, accSafe2, 100.0), "SafeTransfer-1");
        Thread safeT2 = new Thread(() -> Account.transferSafe(accSafe2, accSafe1, 150.0), "SafeTransfer-2");

        safeT1.start();
        safeT2.start();
        safeT1.join();
        safeT2.join();

        System.out.println("SUCCESS: Safe transfers completed without deadlock!");
        System.out.printf("Final Balances -> Account 201: $%.2f | Account 202: $%.2f%n",
                accSafe1.balance, accSafe2.balance);

        // -------------------------------------------------------------
        // SECTION 3: Buffer Size Invariant Tracking (Part B)
        // -------------------------------------------------------------
        System.out.println("\n[SECTION 3: Buffer Invariant Verification (0 <= count <= 5)]");
        MonitoredTokenCounter monitoredCounter = new MonitoredTokenCounter();

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 20; i++) monitoredCounter.put(i);
            } catch (InterruptedException ignored) {}
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 20; i++) monitoredCounter.take();
            } catch (InterruptedException ignored) {}
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        System.out.println("Sample Buffer Size Timeline (first 20 state transitions): "
                + monitoredCounter.getHistory().subList(0, Math.min(20, monitoredCounter.getHistory().size())));
        System.out.println("Were any buffer boundaries violated (>5 or <0)? -> "
                + (monitoredCounter.isViolationDetected() ? "VIOLATION DETECTED" : "NO (STRICT INVARIANT MAINTAINED)"));

        // -------------------------------------------------------------
        // SECTION 4: Explanation of 'while' vs 'if'
        // -------------------------------------------------------------
        System.out.println("\n[SECTION 4: Analysis of 'while' vs 'if' in wait() Invariant Checking]");
        System.out.println("1. Spurious Wakeups: The JVM specification allows threads to awaken without notification.");
        System.out.println("2. Shared State Modification: When notifyAll() wakes up multiple consumers,");
        System.out.println("   the first consumer to acquire the lock consumes the item (reducing count to 0).");
        System.out.println("3. Catastrophic Failure with 'if': If 'if' is used, the second awakened thread");
        System.out.println("   proceeds directly to take from an empty buffer without re-evaluating the condition,");
        System.out.println("   leading to negative count or ArrayIndexOutOfBoundsException!");
    }
}
