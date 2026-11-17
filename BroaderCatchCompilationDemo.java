package Task01;

import java.io.FileNotFoundException;

/**
 * Demonstrates the compiler error when a broader catch block precedes a narrower one.
 */
public class BroaderCatchCompilationDemo {
    public static void testOrder() {
        try {
            throw new FileNotFoundException("Card reader file missing");
        } catch (Exception e) {
            System.out.println("Caught by broader Exception block: " + e.getMessage());
        }
        /*
        // UNCOMMENTING THE BELOW BLOCK CAUSES COMPILE-TIME ERROR:
        // "error: exception FileNotFoundException has already been caught"
        catch (FileNotFoundException fnf) {
            System.out.println("Narrow catch: " + fnf.getMessage());
        }
        */
    }

    public static void main(String[] args) {
        testOrder();
    }
}
