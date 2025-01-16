package beans;

import beans.services.AuthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Managed bean for handling email verification processes.
 * This bean is responsible for verifying user email addresses using a token
 * and providing feedback to the user about the verification status.
 * 
 * @author Davide Scaccia - xscaccd00
 */
@Named("verificationBean")
@RequestScoped
public class VerificationBean {
    
    private String token;
    
    @Inject
    private AuthService authService;
    
    /**
     * Verifies the user's email using the provided token.
     * If the token is valid, the user's email is marked as verified, and a success message is displayed.
     * If the token is invalid or expired, an error message is displayed.
     * 
     * @return a navigation outcome string, or null if verification fails
     */
    public String verifyEmail() {
        try {
            if (token == null || token.trim().isEmpty()) { //If the token is null or empty, show an error message
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid Token", "No verification token provided."); //Show an error message
                return null; //Return null
            }
            
            boolean verified = authService.verifyEmail(token); //Verify the email using the token
            if (verified) {
                addMessage(FacesMessage.SEVERITY_INFO, "Success!", "Your email has been verified. You can now log in."); //Show a success message
                return "login?faces-redirect=true"; //Redirect to login page
            } 
            else addMessage(FacesMessage.SEVERITY_ERROR, "Verification Failed", "The verification link is invalid or has expired. Please request a new one."); //Show an error message
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred during verification: " + e.getMessage()); //Show an error message
        }
        return null;
    }
    
    /**
     * Adds a message to the JSF context.
     * 
     * @param severity the severity of the message
     * @param summary  a brief summary of the message
     * @param detail   detailed information about the message
     */
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail)); //Add a message to the JSF context
    }
    
    /**
     * Gets the verification token.
     * 
     * @return the verification token
     */
    public String getToken() {
        return token;
    }
    
    /**
     * Sets the verification token.
     * 
     * @param token the verification token
     */
    public void setToken(String token) {
        this.token = token;
    }
}
