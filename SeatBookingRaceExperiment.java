package Task05;

import java.util.concurrent.atomic.AtomicInteger;

public class SeatBookingRaceExperiment {

    // -------------------------------------------------------------
    // 1. Unsynchronized Version (Vulnerable to Check-Then-Act Race)
    // -------------------------------------------------------------
    static class UnsyncBooking {
        private final boolean[] booked;
        int bookingsMade = 0;

        UnsyncBooking(int seats) { booked = new boolean[seats]; }

        public boolean book(int seat) {
            // Check-Then-Act Race Condition
            if (!booked[seat]) {
                try {
                    // Small thread yield to provoke race condition reliably
                    Thread.sleep(0, 100);
                } catch (InterruptedException ignored) {}
                booked[seat] = true;
                bookingsMade++;
                return true;
            }
            return false;
        }
    }

    // -------------------------------------------------------------
    // 2. Synchronized Method Version (Coarse-Grained Monitor Lock)
    // -------------------------------------------------------------
    static class SyncMethodBooking {
        private final boolean[] booked;
        int bookingsMade = 0;

        SyncMethodBooking(int seats) { booked = new boolean[seats]; }

        public synchronized boolean book(int seat) {
            if (!booked[seat]) {
                try {
                    Thread.sleep(0, 100);
                } catch (InterruptedException ignored) {}
                booked[seat] = true;
                bookingsMade++;
                return true;
            }
            return false;
        }
    }

    // -------------------------------------------------------------
    // 3. Synchronized Block Version (Fine-Grained Per-Seat Lock)
    // -------------------------------------------------------------
    static class SyncBlockBooking {
        private final boolean[] booked;
        private final Object[] seatLocks;
        private final AtomicInteger bookingsMade = new AtomicInteger(0);

        SyncBlockBooking(int seats) {
            booked = new boolean[seats];
            seatLocks = new Object[seats];
            for (int i = 0; i < seats; i++) {
                seatLocks[i] = new Object();
            }
        }

        public boolean book(int seat) {
            // Fine-grained critical section locked on individual seat object
            synchronized (seatLocks[seat]) {
                if (!booked[seat]) {
                    try {
                        Thread.sleep(0, 100);
                    } catch (InterruptedException ignored) {}
                    booked[seat] = true;
                    bookingsMade.incrementAndGet();
                    return true;
                }
            }
            return false;
        }

        public int getBookingsMade() {
            return bookingsMade.get();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("   TASK 05: RACE CONDITIONS IN CINEMA SEAT-BOOKING SYSTEM");
        System.out.println("================================================================================");

        // -------------------------------------------------------------
        // PART 1: 10 Runs of Unsynchronized Code (Demonstrating Race Condition)
        // -------------------------------------------------------------
        System.out.println("\n[PART 1: 10 Runs of UNSYNCHRONIZED Seat Booking (10 seats, 100 threads)]");
        System.out.println("Target: Exactly 10 seats available. Any count > 10 represents Double Booking!");
        System.out.println("Run #   | Seats Sold | Double Bookings Detected");
        System.out.println("--------|------------|-------------------------");

        int totalDoubleBookings = 0;
        for (int run = 1; run <= 10; run++) {
            UnsyncBooking unsync = new UnsyncBooking(10);
            Thread[] threads = new Thread[100];
            for (int i = 0; i < threads.length; i++) {
                final int seat = i % 10;
                threads[i] = new Thread(() -> unsync.book(seat));
                threads[i].start();
            }
            for (Thread t : threads) t.join();

            int excess = unsync.bookingsMade - 10;
            totalDoubleBookings += excess;
            System.out.printf("Run %-3d | %-10d | %s%n",
                    run, unsync.bookingsMade, (excess > 0 ? "+" + excess + " DOUBLE BOOKED!" : "None"));
        }
        System.out.printf("Total Overbooked Seats Across 10 Runs: %d%n", totalDoubleBookings);

        // -------------------------------------------------------------
        // PART 2: 10 Runs of Synchronized Method (Verified Zero Double Bookings)
        // -------------------------------------------------------------
        System.out.println("\n[PART 2: 10 Runs of SYNCHRONIZED METHOD (10 seats, 100 threads)]");
        System.out.println("Run #   | Seats Sold | Overbooking Status");
        System.out.println("--------|------------|-------------------");
        for (int run = 1; run <= 10; run++) {
            SyncMethodBooking syncMethod = new SyncMethodBooking(10);
            Thread[] threads = new Thread[100];
            for (int i = 0; i < threads.length; i++) {
                final int seat = i % 10;
                threads[i] = new Thread(() -> syncMethod.book(seat));
                threads[i].start();
            }
            for (Thread t : threads) t.join();
            System.out.printf("Run %-3d | %-10d | %s%n",
                    run, syncMethod.bookingsMade, (syncMethod.bookingsMade == 10 ? "PASSED (No Double Booking)" : "FAILED"));
        }

        // -------------------------------------------------------------
        // PART 3: 10 Runs of Synchronized Block (Fine-Grained Seat Locks)
        // -------------------------------------------------------------
        System.out.println("\n[PART 3: 10 Runs of FINE-GRAINED SYNCHRONIZED BLOCK (10 seats, 100 threads)]");
        System.out.println("Run #   | Seats Sold | Overbooking Status");
        System.out.println("--------|------------|-------------------");
        for (int run = 1; run <= 10; run++) {
            SyncBlockBooking syncBlock = new SyncBlockBooking(10);
            Thread[] threads = new Thread[100];
            for (int i = 0; i < threads.length; i++) {
                final int seat = i % 10;
                threads[i] = new Thread(() -> syncBlock.book(seat));
                threads[i].start();
            }
            for (Thread t : threads) t.join();
            System.out.printf("Run %-3d | %-10d | %s%n",
                    run, syncBlock.getBookingsMade(), (syncBlock.getBookingsMade() == 10 ? "PASSED (No Double Booking)" : "FAILED"));
        }

        // -------------------------------------------------------------
        // PART 4: Throughput Benchmark (10,000 Bookings)
        // -------------------------------------------------------------
        System.out.println("\n[PART 4: Performance Benchmark - 10,000 Concurrent Bookings across 50 seats]");
        final int BENCHMARK_SEATS = 50;
        final int TOTAL_BOOKINGS = 10000;

        // A. Synchronized Method Benchmark
        long startSyncMethod = System.nanoTime();
        SeatBooking methodScreen = new SeatBooking(BENCHMARK_SEATS);
        Thread[] benchThreads1 = new Thread[TOTAL_BOOKINGS];
        for (int i = 0; i < TOTAL_BOOKINGS; i++) {
            final int s = i % BENCHMARK_SEATS;
            benchThreads1[i] = new Thread(() -> methodScreen.book(s));
            benchThreads1[i].start();
        }
        for (Thread t : benchThreads1) t.join();
        long durSyncMethodMs = (System.nanoTime() - startSyncMethod) / 1_000_000;

        // B. Fine-Grained Synchronized Block Benchmark
        long startSyncBlock = System.nanoTime();
        SyncBlockBooking blockScreen = new SyncBlockBooking(BENCHMARK_SEATS);
        Thread[] benchThreads2 = new Thread[TOTAL_BOOKINGS];
        for (int i = 0; i < TOTAL_BOOKINGS; i++) {
            final int s = i % BENCHMARK_SEATS;
            benchThreads2[i] = new Thread(() -> blockScreen.book(s));
            benchThreads2[i].start();
        }
        for (Thread t : benchThreads2) t.join();
        long durSyncBlockMs = (System.nanoTime() - startSyncBlock) / 1_000_000;

        System.out.printf("Synchronized Method (Coarse-Grained) : %d ms (Seats Sold: %d)%n",
                durSyncMethodMs, methodScreen.getBookingsMade());
        System.out.printf("Synchronized Block  (Fine-Grained)   : %d ms (Seats Sold: %d)%n",
                durSyncBlockMs, blockScreen.getBookingsMade());
    }
}
