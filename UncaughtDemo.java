package Task01;

/**
 * Demonstrates an uncaught exception to inspect and explain each line of the stack trace.
 */
public class UncaughtDemo {
    public static void methodC() {
        // Intentionally trigger an uncaught ArithmeticException
        int result = 100 / 0;
        System.out.println("Result: " + result);
    }

    public static void methodB() {
        methodC();
    }

    public static void methodA() {
        methodB();
    }

    public static void main(String[] args) {
        System.out.println("Initiating execution chain for stack trace demonstration...");
        methodA();
    }
}
