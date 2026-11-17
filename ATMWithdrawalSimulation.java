package Task01;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Custom checked exception for insufficient account balance.
 */
class InsufficientFundsException extends Exception {
    private final double balance;
    private final double requested;

    public InsufficientFundsException(double balance, double requested) {
        super(String.format("Insufficient funds: Available $%.2f, Requested $%.2f", balance, requested));
        this.balance = balance;
        this.requested = requested;
    }

    public double getBalance() { return balance; }
    public double getRequested() { return requested; }
}

/**
 * Custom unchecked exception for invalid withdrawal parameters (e.g. negative amount).
 */
class InvalidAmountException extends IllegalArgumentException {
    public InvalidAmountException(String message) {
        super(message);
    }
}

/**
 * ATM Withdrawal Simulation demonstrating:
 * 1. Identification of failure points
 * 2. Multi-catch usage
 * 3. Specific vs. broad catch ordering
 * 4. Guaranteed execution via finally
 */
public class ATMWithdrawalSimulation {

    private static final String CARD_READER_CONFIG = "Task01/card_reader.dat";
    private double balance = 1000.00;

    /**
     * Verifies the hardware card reader configuration file.
     * Throws checked FileNotFoundException if missing.
     */
    public void verifyCardReader(String configPath) throws FileNotFoundException {
        File file = new File(configPath);
        if (!file.exists()) {
            throw new FileNotFoundException("Card-reader hardware configuration missing at: " + configPath);
        }
    }

    /**
     * Performs withdrawal transaction.
     * Demonstrates try, multi-catch, and finally.
     */
    public boolean withdraw(String accStr, String amountStr, String configPath) {
        System.out.println("\n--------------------------------------------------");
        System.out.println("Processing Withdrawal Request:");
        System.out.println("Account: " + accStr + " | Amount: " + amountStr + " | Config: " + configPath);
        System.out.println("Current Balance: $" + balance);

        boolean sessionActive = true;
        boolean transactionSuccess = false;

        try {
            // Point 1: Hardware check (Checked Exception)
            verifyCardReader(configPath);

            // Point 2: Parsing input (Unchecked Exception: NumberFormatException)
            int accountNumber = Integer.parseInt(accStr);
            double amount = Double.parseDouble(amountStr);

            // Point 3: Business rule validation (Unchecked Exception: InvalidAmountException)
            if (amount <= 0) {
                throw new InvalidAmountException("Withdrawal amount must be strictly positive. Given: " + amount);
            }

            // Point 4: Balance validation (Checked Exception: InsufficientFundsException)
            if (amount > balance) {
                throw new InsufficientFundsException(balance, amount);
            }

            // Dispense Cash
            balance -= amount;
            transactionSuccess = true;
            System.out.println("SUCCESS: Dispensed $" + amount + ". New Balance: $" + balance);

        } catch (FileNotFoundException e) {
            // Checked Exception: Specific catch
            System.out.println("[CATCH - FileNotFoundException]: Hardware fault - " + e.getMessage());
        } catch (NumberFormatException | InvalidAmountException e) {
            // Multi-catch: Handling both invalid formats and illegal amounts uniformly
            System.out.println("[CATCH - Multi-Catch (Input Validation)]: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        } catch (InsufficientFundsException e) {
            // Specific business checked exception
            System.out.println("[CATCH - InsufficientFundsException]: Transaction denied - " + e.getMessage());
        } catch (Exception e) {
            // Catch-all safety net for unexpected runtime errors
            System.out.println("[CATCH - General Exception]: Unexpected error occurred: " + e.getMessage());
        } finally {
            // Guarantees card ejection and session termination regardless of success or failure
            sessionActive = false;
            System.out.println("[FINALLY]: Card ejected. Secure session terminated. (Session Active: " + sessionActive + ")");
        }

        return transactionSuccess;
    }

    public static void main(String[] args) {
        ATMWithdrawalSimulation atm = new ATMWithdrawalSimulation();

        // Ensure card reader file exists for positive test cases
        File cardFile = new File(CARD_READER_CONFIG);
        try {
            if (!cardFile.getParentFile().exists()) {
                cardFile.getParentFile().mkdirs();
            }
            if (!cardFile.exists()) {
                java.nio.file.Files.writeString(cardFile.toPath(), "CARD_READER_READY:PORT_COM3");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("==================================================");
        System.out.println("     ATM SIMULATION TEST SUITE - 5 DISTINCT PATHS ");
        System.out.println("==================================================");

        // Path 1: Valid input and successful cash dispensation
        System.out.println("\n[TEST CASE 1]: Valid Withdrawal");
        atm.withdraw("123456", "200.00", CARD_READER_CONFIG);

        // Path 2: Non-numeric account input (NumberFormatException)
        System.out.println("\n[TEST CASE 2]: Non-numeric Input");
        atm.withdraw("ACC_XYZ", "100.00", CARD_READER_CONFIG);

        // Path 3: Negative withdrawal amount (InvalidAmountException)
        System.out.println("\n[TEST CASE 3]: Negative Withdrawal Amount");
        atm.withdraw("123456", "-50.00", CARD_READER_CONFIG);

        // Path 4: Request exceeding balance (InsufficientFundsException)
        System.out.println("\n[TEST CASE 4]: Amount Exceeding Balance");
        atm.withdraw("123456", "5000.00", CARD_READER_CONFIG);

        // Path 5: Missing card reader hardware file (FileNotFoundException)
        System.out.println("\n[TEST CASE 5]: Missing Card Reader File");
        atm.withdraw("123456", "100.00", "Task01/missing_reader.dat");

        // Demonstration of Broader vs Narrower Catch block compilation behavior
        System.out.println("\n==================================================");
        System.out.println("  DEMONSTRATION: Broader Catch Before Narrower   ");
        System.out.println("==================================================");
        System.out.println("In Java, placing a broader catch (e.g. catch (Exception e)) before a");
        System.out.println("narrower catch (e.g. catch (FileNotFoundException e)) results in a");
        System.out.println("COMPILATION ERROR: 'exception java.io.FileNotFoundException has already been caught'.");
        System.out.println("This is enforced by JLS 14.20: Unreachable catch clause analysis.");
    }
}
