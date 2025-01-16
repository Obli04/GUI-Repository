package beans.services;

/**
 * Represents the result of a registration process.
 * This class encapsulates the success status, email notification status,
 * and a message providing additional information about the registration outcome.
 * 
 * @author Davide Scaccia - xscaccd00
 */
public class RegistrationResult {
    private final boolean success;
    private final boolean emailSent;
    private final String message;

    /**
     * Constructs a new RegistrationResult with the specified details.
     * 
     * @param success   whether the registration was successful
     * @param emailSent whether an email was sent as part of the registration process
     * @param message   a message providing additional information about the registration outcome
     */
    public RegistrationResult(boolean success, boolean emailSent, String message) {
        this.success = success;
        this.emailSent = emailSent;
        this.message = message;
    }

    /**
     * Returns whether the registration was successful.
     * 
     * @return true if the registration was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns whether an email was sent as part of the registration process.
     * 
     * @return true if an email was sent, false otherwise
     */
    public boolean isEmailSent() {
        return emailSent;
    }

    /**
     * Returns the message providing additional information about the registration outcome.
     * 
     * @return the registration outcome message
     */
    public String getMessage() {
        return message;
    }
}
