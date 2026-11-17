package Task03;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class DailySalesComparisonDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   TASK 03: RESOURCE SAFETY & DEBUGGING - DAILY SALES REPORT GENERATOR");
        System.out.println("================================================================================");

        List<String> validFiles = Arrays.asList(
                "Task03/stores/store1.csv",
                "Task03/stores/store2.csv",
                "Task03/stores/store4.csv"
        );

        List<String> failingFiles = Arrays.asList(
                "Task03/stores/store1.csv",
                "Task03/stores/store2.csv",
                "Task03/stores/store3_corrupt.csv",
                "Task03/stores/store4.csv"
        );

        String manualOut = "Task03/report_manual.csv";
        String twrOut = "Task03/report_twr.csv";

        // Experiment 1: Valid Execution - Verify both produce identical outputs
        System.out.println("\n[EXPERIMENT 1]: Running on Valid Store Files");
        try {
            DailySalesReportManual.generateReport(validFiles, manualOut);
            DailySalesReportTWR.generateReport(validFiles, twrOut);

            byte[] manualBytes = Files.readAllBytes(new File(manualOut).toPath());
            byte[] twrBytes = Files.readAllBytes(new File(twrOut).toPath());

            // Normalize header text for comparison
            String manualContent = new String(manualBytes).replace("=== DAILY SALES SUMMARY (MANUAL CLEANUP) ===", "SUMMARY");
            String twrContent = new String(twrBytes).replace("=== DAILY SALES SUMMARY (TRY-WITH-RESOURCES) ===", "SUMMARY");

            boolean contentsMatch = manualContent.trim().equals(twrContent.trim());
            System.out.println("VERIFICATION: Do both versions produce identical reports? -> " + (contentsMatch ? "YES (MATCH)" : "NO"));
        } catch (Exception e) {
            System.out.println("Unexpected failure in valid execution: " + e.getMessage());
        }

        // Experiment 2: Failing Execution - Inject corrupt record and count leaked handles
        System.out.println("\n[EXPERIMENT 2]: Injecting Corrupt Input ('store3_corrupt.csv')");

        System.out.println("\n--- Testing Manual Resource Version Under Failure ---");
        try {
            DailySalesReportManual.generateReport(failingFiles, "Task03/report_manual_failed.csv");
        } catch (IOException e) {
            System.out.println("Expected Catch in Caller: " + e.getMessage());
            System.out.println("EVIDENCE: Open File Handles Remaining (Manual): " + DailySalesReportManual.openFileHandlesCount);
            System.out.println("EXPLANATION: Manual reader leaked handle because NumberFormatException bypassed reader.close()!");
        }

        System.out.println("\n--- Testing Try-With-Resources Version Under Failure ---");
        try {
            DailySalesReportTWR.generateReport(failingFiles, "Task03/report_twr_failed.csv");
        } catch (IOException e) {
            System.out.println("Expected Catch in Caller: " + e.getMessage());
            System.out.println("EVIDENCE: Open File Handles Remaining (TWR): " + DailySalesReportTWR.openFileHandlesCount);
            System.out.println("EXPLANATION: TWR automatically invoked AutoCloseable.close() via compiler synthetic finally!");
        }

        // Experiment 3: Suppressed Exceptions
        DailySalesReportTWR.demonstrateSuppressedExceptions();

        System.out.println("\n================================================================================");
        System.out.println("  SUMMARY TABLE: RESOURCE SAFETY COMPARISON UNDER EXCEPTION");
        System.out.println("================================================================================");
        System.out.println("| Metric / Behavior            | Manual try-catch-finally | Try-With-Resources (TWR) |");
        System.out.println("|------------------------------|--------------------------|--------------------------|");
        System.out.println("| Handles left open on error   | 1 (Leaked file handle)   | 0 (Guaranteed closed)    |");
        System.out.println("| Handling of close() error    | Overwrites primary error | Preserved as Suppressed  |");
        System.out.println("| Close ordering               | Manual / error-prone     | Strict LIFO order        |");
        System.out.println("| Code verbosity               | High (nested try-finally)| Minimal & elegant        |");
        System.out.println("================================================================================");
    }
}
