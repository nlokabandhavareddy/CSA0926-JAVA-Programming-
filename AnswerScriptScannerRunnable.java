package Task04;

/**
 * Scanner station implemented by implementing the Runnable interface.
 * Decouples the task logic from the execution thread mechanism.
 */
public class AnswerScriptScannerRunnable implements Runnable {
    private final int stationId;
    private final int scriptCount;
    private final long delayMs;
    private int scannedCount = 0;

    public AnswerScriptScannerRunnable(int stationId, int scriptCount, long delayMs) {
        this.stationId = stationId;
        this.scriptCount = scriptCount;
        this.delayMs = delayMs;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        System.out.printf("[%s | Station-%d] Started scanning %d scripts...%n",
                threadName, stationId, scriptCount);
        try {
            for (int i = 1; i <= scriptCount; i++) {
                Thread.sleep(delayMs);
                scannedCount++;
            }
        } catch (InterruptedException e) {
            System.err.printf("[%s | Station-%d] Interrupted!%n", threadName, stationId);
            Thread.currentThread().interrupt();
        }
        System.out.printf("[%s | Station-%d] COMPLETED %d scripts.%n",
                threadName, stationId, scannedCount);
    }

    public int getScannedCount() {
        return scannedCount;
    }

    public int getStationId() {
        return stationId;
    }
}
