package Task03;

import java.io.*;
import java.util.*;

/**
 * Version 2: Modern Resource Management using Java try-with-resources (TWR).
 * Implements AutoCloseable semantics, deterministic LIFO closing, zero leaks even on failure,
 * and preserves suppressed exceptions.
 */
public class DailySalesReportTWR {

    public static int openFileHandlesCount = 0;

    /**
     * Monitored BufferedReader that allows tracking open file handle counts.
     */
    static class MonitoredReader extends BufferedReader {
        private boolean closed = false;

        public MonitoredReader(Reader in) {
            super(in);
            openFileHandlesCount++;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                openFileHandlesCount--;
                super.close();
            }
        }
    }

    public static void generateReport(List<String> filePaths, String outputReportPath) throws IOException {
        openFileHandlesCount = 0;

        // Try-with-resources for the output writer
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputReportPath))) {
            openFileHandlesCount++; // Writer tracked
            writer.write("=== DAILY SALES SUMMARY (TRY-WITH-RESOURCES) ===\n");
            writer.write("Item,TotalQuantity,TotalRevenue\n");

            Map<String, Integer> totalQuantities = new LinkedHashMap<>();
            Map<String, Double> totalRevenues = new LinkedHashMap<>();

            for (String filePath : filePaths) {
                // Try-with-resources for each input file: GUARANTEED to close upon block exit
                try (MonitoredReader reader = new MonitoredReader(new FileReader(filePath))) {
                    System.out.println("[TWR]: Processing " + filePath + " | Currently open handles: " + openFileHandlesCount);

                    String line = reader.readLine(); // Header
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        String item = parts[0].trim();
                        // Corrupt data triggers NumberFormatException
                        int qty = Integer.parseInt(parts[1].trim());
                        double price = Double.parseDouble(parts[2].trim());

                        totalQuantities.put(item, totalQuantities.getOrDefault(item, 0) + qty);
                        totalRevenues.put(item, totalRevenues.getOrDefault(item, 0.0) + (qty * price));
                    }
                } catch (NumberFormatException nfe) {
                    System.out.println("[LOG - TWR FAILURE IN FILE]: " + filePath + " caused parsing error: " + nfe.getMessage());
                    // Note: MonitoredReader is ALREADY closed before entering this catch block!
                    throw new IOException("Failed processing " + filePath + " due to corrupt record", nfe);
                }
            }

            for (String item : totalQuantities.keySet()) {
                writer.write(String.format("%s,%d,%.2f\n", item, totalQuantities.get(item), totalRevenues.get(item)));
            }
            writer.flush();
            System.out.println("[TWR]: Report generated successfully at: " + outputReportPath);
        } finally {
            openFileHandlesCount--; // Writer closed automatically
        }
    }

    /**
     * Demonstrates how TWR handles suppressed exceptions when close() throws an error.
     */
    public static void demonstrateSuppressedExceptions() {
        System.out.println("\n--- DEMONSTRATION OF SUPPRESSED EXCEPTIONS ---");
        class FaultyResource implements AutoCloseable {
            private final String name;
            public FaultyResource(String name) { this.name = name; }
            public void doWork() throws IOException {
                throw new IOException("Primary failure in " + name + " while writing report data");
            }
            @Override
            public void close() throws IOException {
                throw new IOException("Secondary failure while closing " + name);
            }
        }

        try (FaultyResource res = new FaultyResource("SalesOutputChannel")) {
            res.doWork();
        } catch (IOException e) {
            System.out.println("Caught Primary Exception: " + e.getMessage());
            Throwable[] suppressed = e.getSuppressed();
            System.out.println("Suppressed Exceptions Count: " + suppressed.length);
            for (Throwable s : suppressed) {
                System.out.println(" -> Suppressed: " + s.getMessage());
            }
        }
    }
}
