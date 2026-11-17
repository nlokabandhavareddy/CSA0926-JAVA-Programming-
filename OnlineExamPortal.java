package Task02;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// ==========================================
// Custom Exception Hierarchy
// ==========================================

// Base Checked Exception for Examination domain
class ExamPortalException extends Exception {
    public ExamPortalException(String message) {
        super(message);
    }
    public ExamPortalException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Layer 1 (Controller) Exception: Checked because window timing is an anticipated operational condition
class ExamWindowClosedException extends ExamPortalException {
    public ExamWindowClosedException(String message) {
        super(message);
    }
}

// Layer 2 (Service) Exception: Checked because student registration must be explicitly recovered or verified
class StudentNotRegisteredException extends ExamPortalException {
    private final String studentId;
    private final String courseCode;

    public StudentNotRegisteredException(String studentId, String courseCode) {
        super(String.format("Student '%s' is not registered for course '%s'.", studentId, courseCode));
        this.studentId = studentId;
        this.courseCode = courseCode;
    }

    public String getStudentId() { return studentId; }
    public String getCourseCode() { return courseCode; }
}

// Layer 3 (Storage) Exception: Checked wrapper around low-level I/O errors (exception chaining)
class StorageLayerException extends ExamPortalException {
    public StorageLayerException(String message, Throwable cause) {
        super(message, cause);
    }
}

// ==========================================
// Three-Layer Architecture
// ==========================================

// 1. Storage Layer
class ExamStorageService {
    private static final long MAX_FILE_SIZE_KB = 500;

    public void storeAnswerScript(String filename, long sizeKb) throws StorageLayerException {
        try {
            if (sizeKb > MAX_FILE_SIZE_KB) {
                // Low-level I/O / disk quota simulation
                throw new IOException("Disk quota exceeded: script size " + sizeKb + "KB exceeds limit of " + MAX_FILE_SIZE_KB + "KB");
            }
            System.out.println("   [StorageLayer]: Successfully persisted " + filename + " (" + sizeKb + "KB)");
        } catch (IOException ioe) {
            // Chaining: Wrap IOException into StorageLayerException preserving root cause
            throw new StorageLayerException("Storage failure while saving answer script '" + filename + "'", ioe);
        }
    }
}

// 2. Service Layer
class ExamBusinessService {
    private final ExamStorageService storageService = new ExamStorageService();
    private final Set<String> registeredStudents = new HashSet<>();

    public ExamBusinessService() {
        registeredStudents.add("192524027");
        registeredStudents.add("STU_1002");
    }

    public void validateAndProcessSubmission(String studentId, String courseCode, String filename, long sizeKb)
            throws StudentNotRegisteredException, StorageLayerException {
        // Business rule check
        if (!registeredStudents.contains(studentId)) {
            throw new StudentNotRegisteredException(studentId, courseCode);
        }

        System.out.println("   [ServiceLayer]: Student " + studentId + " verified. Delegating to storage layer...");
        storageService.storeAnswerScript(filename, sizeKb);
    }
}

// 3. Controller Layer
public class OnlineExamPortal {
    private final ExamBusinessService businessService = new ExamBusinessService();
    private final LocalDateTime windowCloseTime = LocalDateTime.now().plusHours(1);

    public void submitExam(String studentId, String courseCode, String filename, long sizeKb, boolean simulateClosedWindow) {
        System.out.println("\n=======================================================");
        System.out.printf("CONTROLLER: Received submission from Student: %s for %s%n", studentId, courseCode);
        System.out.println("=======================================================");

        try {
            // Check 1: Controller-level timing rule
            if (simulateClosedWindow) {
                throw new ExamWindowClosedException("Submission rejected: Exam window closed at " + windowCloseTime.minusMinutes(10));
            }

            // Propagate call through service and storage layers
            businessService.validateAndProcessSubmission(studentId, courseCode, filename, sizeKb);
            System.out.println("RESULT: Submission SUCCESSFUL for " + studentId);

        } catch (ExamWindowClosedException ewce) {
            System.out.println("[HANDLED IN CONTROLLER]: " + ewce.getMessage());
            System.out.println("USER FEEDBACK: 'Exam window has expired. Contact exam controller.'");

        } catch (StudentNotRegisteredException snre) {
            System.out.println("[HANDLED IN CONTROLLER]: " + snre.getMessage());
            System.out.println("USER FEEDBACK: 'Access denied: You are not enrolled in course " + snre.getCourseCode() + ".'");

        } catch (StorageLayerException sle) {
            System.out.println("[HANDLED IN CONTROLLER]: " + sle.getMessage());
            System.out.println("CHAINED CAUSE: " + sle.getCause().getClass().getName() + " -> " + sle.getCause().getMessage());
            System.out.println("USER FEEDBACK: 'Submission failed due to a server storage limit. Please compress file below 500KB.'");

        }
    }

    public static void demonstrateChainingLoss() {
        System.out.println("\n-------------------------------------------------------");
        System.out.println("DEMONSTRATION: Re-throwing WITH vs WITHOUT Cause");
        System.out.println("-------------------------------------------------------");

        // Case A: With cause
        try {
            try {
                throw new IOException("Underlying disk cluster read timeout");
            } catch (IOException cause) {
                throw new StorageLayerException("Wrapped with cause", cause);
            }
        } catch (StorageLayerException e) {
            System.out.println("1. With Cause preserved:");
            System.out.println("   Exception: " + e);
            System.out.println("   Root Cause: " + e.getCause());
        }

        // Case B: Without cause
        try {
            try {
                throw new IOException("Underlying disk cluster read timeout");
            } catch (IOException cause) {
                throw new StorageLayerException("Wrapped WITHOUT cause", null);
            }
        } catch (StorageLayerException e) {
            System.out.println("2. Without Cause (information lost):");
            System.out.println("   Exception: " + e);
            System.out.println("   Root Cause: " + e.getCause() + " (Diagnosis information completely severed!)");
        }
    }

    public static void main(String[] args) {
        OnlineExamPortal portal = new OnlineExamPortal();

        // Scenario 1: Successful submission
        System.out.println("[SCENARIO 1: Valid Submission]");
        portal.submitExam("192524027", "CSA1714", "ai_answer_sheet.pdf", 340, false);

        // Scenario 2: Window Closed Exception (Controller Layer)
        System.out.println("\n[SCENARIO 2: Window Closed]");
        portal.submitExam("192524027", "CSA1714", "ai_answer_sheet.pdf", 340, true);

        // Scenario 3: Student Not Registered Exception (Service Layer)
        System.out.println("\n[SCENARIO 3: Unregistered Student]");
        portal.submitExam("UNKNOWN_STUDENT_99", "CSA1714", "notes.pdf", 200, false);

        // Scenario 4: Storage Limit Exceeded / Quota (Storage Layer chained)
        System.out.println("\n[SCENARIO 4: File Exceeds Limit (Chained Exception)]");
        portal.submitExam("192524027", "CSA1714", "large_scanned_book.pdf", 950, false);

        // Chaining evidence demonstration
        demonstrateChainingLoss();
    }
}
