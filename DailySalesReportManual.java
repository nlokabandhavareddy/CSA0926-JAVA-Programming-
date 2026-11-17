package Task03;

import java.io.*;
import java.util.*;

/**
 * Version 1: Manual Resource Management using try-catch-finally.
 * Exhibits classic pitfalls: file descriptor leaks on unexpected parsing failure,
 * unclosed streams if an exception aborts the loop before close(), and suppressed close errors.
 */
public class DailySalesReportManual {

    public static int openFileHandlesCount = 0;

    public static void generateReport(List<String> filePaths, String outputReportPath) throws IOException {
        BufferedWriter writer = null;
        openFileHandlesCount = 0;

        try {
            writer = new BufferedWriter(new FileWriter(outputReportPath));
            openFileHandlesCount++; // Writer opened
            writer.write("=== DAILY SALES SUMMARY (MANUAL CLEANUP) ===\n");
            writer.write("Item,TotalQuantity,TotalRevenue\n");

            Map<String, Integer> totalQuantities = new LinkedHashMap<>();
            Map<String, Double> totalRevenues = new LinkedHashMap<>();

            for (String filePath : filePaths) {
                BufferedReader reader = null;
                try {
                    File file = new File(filePath);
                    reader = new BufferedReader(new FileReader(file));
                    openFileHandlesCount++; // Reader opened
                    System.out.println("[Manual]: Processing " + filePath + " | Currently open handles: " + openFileHandlesCount);

                    String line = reader.readLine(); // Header
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        String item = parts[0].trim();
                        // FAILURE POINT: If input is corrupt, NumberFormatException is thrown here!
                        int qty = Integer.parseInt(parts[1].trim());
                        double price = Double.parseDouble(parts[2].trim());

                        totalQuantities.put(item, totalQuantities.getOrDefault(item, 0) + qty);
                        totalRevenues.put(item, totalRevenues.getOrDefault(item, 0.0) + (qty * price));
                    }

                    // MANUAL CLOSE: If an exception occurred above, this line is NEVER REACHED!
                    reader.close();
                    openFileHandlesCount--; // Successfully closed
                } catch (NumberFormatException nfe) {
                    // Logging which file caused the failure
                    System.out.println("[LOG - FAILURE IN FILE]: " + filePath + " caused parsing error: " + nfe.getMessage());
                    // NOTE: reader is NOT closed here in naive manual code! It leaks!
                    throw new IOException("Failed processing " + filePath + " due to corrupt record", nfe);
                }
            }

            // Write totals
            for (String item : totalQuantities.keySet()) {
                writer.write(String.format("%s,%d,%.2f\n", item, totalQuantities.get(item), totalRevenues.get(item)));
            }
            writer.flush();
            System.out.println("[Manual]: Report generated successfully at: " + outputReportPath);

        } finally {
            if (writer != null) {
                try {
                    writer.close();
                    openFileHandlesCount--;
                } catch (IOException ignored) {}
            }
        }
    }
}
