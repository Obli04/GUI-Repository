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
    
    public String verifyEmail() {
        try {
            if (token == null || token.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid Token", "No verification token provided.");
                return null;
            }
            
            boolean verified = authService.verifyEmail(token);
            if (verified) {
                addMessage(FacesMessage.SEVERITY_INFO, "Success!", "Your email has been verified. You can now log in.");
                return "login?faces-redirect=true"; // Redirect to login page
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Verification Failed", "The verification link is invalid or has expired. Please request a new one.");
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred during verification: " + e.getMessage());
        }
        return null;
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
