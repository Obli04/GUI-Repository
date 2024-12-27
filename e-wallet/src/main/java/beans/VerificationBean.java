package beans;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import beans.services.AuthService;

@Named("verificationBean")
@RequestScoped
public class VerificationBean {
    
    private String token;
    
    @Inject
    private AuthService authService;
    
    public void verifyEmail() {
        try {
            System.out.println("Verifying email with token: " + token);
            
            if (token == null || token.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid Token", 
                    "No verification token provided.");
                return;
            }
            
            boolean verified = authService.verifyEmail(token);
            if (verified) {
                addMessage(FacesMessage.SEVERITY_INFO, "Success!", 
                    "Your email has been verified. You can now log in.");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Verification Failed", 
                    "The verification link is invalid or has expired. Please request a new one.");
            }
            
            System.out.println("Email verification result: " + verified);
            
        } catch (Exception e) {
            System.err.println("Error during email verification: " + e.getMessage());
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred during verification: " + e.getMessage());
        }
    }
    
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }
    
    // Getters and setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        System.out.println("Setting verification token: " + token);
        this.token = token;
    }
}
