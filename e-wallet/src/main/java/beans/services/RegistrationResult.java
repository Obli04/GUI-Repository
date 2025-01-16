package beans.services;

/**
 * Represents the result of a registration process.
 * This class encapsulates the success status, email notification status,
 * and a message providing additional information about the registration outcome.
 * 
 * @author Davide Scaccia - xscaccd00
 */
public class RegistrationResult {
    private boolean success;
    private boolean emailSent;
    private String message;

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

    /**
     * Sets the message.
     * 
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Sets the email sent.
     * 
     * @param emailSent the email sent to set
     */
    public void setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
    }

    /**
     * Sets the success.
     * 
     * @param success the success to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
