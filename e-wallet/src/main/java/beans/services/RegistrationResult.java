package beans.services;

public class RegistrationResult {
    private final boolean success;
    private final boolean emailSent;
    private final String message;

    public RegistrationResult(boolean success, boolean emailSent, String message) {
        this.success = success;
        this.emailSent = emailSent;
        this.message = message;
    }

    // Add getters
    public boolean isSuccess() { return success; }
    public boolean isEmailSent() { return emailSent; }
    public String getMessage() { return message; }
}
