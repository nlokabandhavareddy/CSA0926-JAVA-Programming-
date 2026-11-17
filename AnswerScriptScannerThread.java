package Task04;

/**
 * Scanner station implemented by extending the Thread class.
 */
public class AnswerScriptScannerThread extends Thread {
    private final int stationId;
    private final int scriptCount;
    private final long delayMs;
    private int scannedCount = 0;

    public AnswerScriptScannerThread(int stationId, int scriptCount, long delayMs) {
        super("ScannerStation-Thread-" + stationId);
        this.stationId = stationId;
        this.scriptCount = scriptCount;
        this.delayMs = delayMs;
    }

    @Override
    public void run() {
        System.out.printf("[%s] Started scanning %d scripts... (State: %s)%n",
                getName(), scriptCount, getState());
        try {
            for (int i = 1; i <= scriptCount; i++) {
                // Simulate physical scanning delay
                Thread.sleep(delayMs);
                scannedCount++;
            }
        } catch (InterruptedException e) {
            System.err.printf("[%s] Scanning interrupted!%n", getName());
            Thread.currentThread().interrupt();
        }
        System.out.printf("[%s] FINISHED. Scanned %d scripts.%n", getName(), scannedCount);
    }

    public int getScannedCount() {
        return scannedCount;
    }

    public int getStationId() {
        return stationId;
    }
}
