package beans;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Base64;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import beans.entities.User;
import beans.services.AuthService;
import beans.services.RegistrationResult;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;

@Named
@SessionScoped
public class UserBean implements Serializable {
    private static final long serialVersionUID = 1L;

    // Form fields
    private String email;
    private String password;
    private String firstName;
    private String secondName;
    private String twoFactorCode;
    private Double balance;
    private Double budget;
    private Double piggyBank;
    
    // Session data
    private User currentUser;
    private StreamedContent qrCodeImage;
    
    @Inject
    private AuthService authService;

    private String message;
    private String messageStyle;
    
    @Inject
    private FacesContext facesContext;
    
    @Inject
    private DepositBean depositBean;

    // Login method
    public String login() {
        try {
            boolean isAuthenticated = authService.login(email, password, twoFactorCode);
            if (isAuthenticated) {
                // Retrieve the authenticated user
                currentUser = authService.findUserByEmail(email);
                
                if (currentUser != null) {
                    // Set names and other fields
                    this.firstName = currentUser.getFirstName();
                    this.secondName = currentUser.getSecondName();
                    this.email = currentUser.getEmail();
                    this.balance = currentUser.getBalance();
                    this.budget = currentUser.getBudget();
                    this.piggyBank = currentUser.getPiggyBank();
                    
                    // Store in session with attribute name "userBean" for consistency
                    FacesContext facesContext = FacesContext.getCurrentInstance();
                    HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(true);
                    session.setAttribute("userBean", this);
                    
                    // Initialize deposit information
                    depositBean.initializeDeposit();
                    
                    // Redirect to dashboard
                    return "dashboard.xhtml?faces-redirect=true";
                } else {
                    FacesContext.getCurrentInstance().addMessage(null, 
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login Failed", "User data not found."));
                    return null;
                }
            } else {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login Failed", "Invalid credentials or 2FA code."));
                return null;
            }
        } catch (SecurityException se) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login Error", se.getMessage()));
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login Error", "An unexpected error occurred."));
            return null;
        }
    }

    // Register method
    public String register() {
        try {
            System.out.println("\n=== Starting Registration Process ===");
            System.out.println("Registering email: " + email);
            
            if (!isValidEmail(email)) {
                addErrorMessage("Registration failed", "Please enter a valid email address");
                return null;
            }

            authService.validatePassword(password);
           
            User newUser = new User();
            newUser.setEmail(email.trim().toLowerCase());
            
            newUser.setPassword(password);
            newUser.setFirstName(firstName.trim());
            newUser.setSecondName(secondName.trim());
            
            if (authService.findUserByEmail(newUser.getEmail()) != null) {
                addErrorMessage("Registration failed", "An account with this email already exists");
                return null;
            }
            
            RegistrationResult result = authService.register(newUser);
            
            // Store the result message in the session for display after redirect
            FacesContext context = FacesContext.getCurrentInstance();
            context.getExternalContext().getSessionMap().put("registrationMessage", result.getMessage());
            context.getExternalContext().getSessionMap().put("emailSent", result.isEmailSent());
            
            System.out.println("Registration completed - Email sent: " + result.isEmailSent());
            System.out.println("Message: " + result.getMessage());
            System.out.println("=== End Registration Process ===\n");
            
            // Force flush the system output before redirect
            System.out.flush();
            
            return "login.xhtml?faces-redirect=true";
            
        } catch (Exception e) {
            System.err.println("Registration failed with error: " + e.getMessage());
            e.printStackTrace();
            addErrorMessage("Registration failed", e.getMessage());
            return null;
        }
    }
 
    // Add this method to handle the registration message after redirect
    public void checkRegistrationMessage() {
        FacesContext context = FacesContext.getCurrentInstance();
        String message = (String) context.getExternalContext().getSessionMap().get("registrationMessage");
        Boolean emailSent = (Boolean) context.getExternalContext().getSessionMap().get("emailSent");
        
        if (message != null) {
            FacesMessage.Severity severity = emailSent ? 
                FacesMessage.SEVERITY_INFO : FacesMessage.SEVERITY_WARN;
            context.addMessage(null, new FacesMessage(severity, "Registration Status", message));
            
            // Clear the session attributes
            context.getExternalContext().getSessionMap().remove("registrationMessage");
            context.getExternalContext().getSessionMap().remove("emailSent");
        }
    }

    // 2FA verification
    public String verify2FA() {
        try {
            if (authService.verify2FA(currentUser, twoFactorCode)) {
                return "dashboard.xhtml?faces-redirect=true";
            }
            addErrorMessage("Verification failed", "Invalid code");
            return null;
        } catch (Exception e) {
            addErrorMessage("Error", e.getMessage());
            return null;
        }
    }

    // QR Code generation for 2FA
    public StreamedContent getQrCodeImage() {
        if (qrCodeImage == null && currentUser != null && 
            currentUser.getTwoFactorSecret() != null) {
            try {
                String otpAuthURL = String.format(
                    "otpauth://totp/CashHive:%s?secret=%s&issuer=CashHive",
                    currentUser.getEmail(),
                    currentUser.getTwoFactorSecret()
                );

                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(
                    otpAuthURL, 
                    BarcodeFormat.QR_CODE, 
                    200, 
                    200
                );

                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(
                    bitMatrix, 
                    "PNG", 
                    pngOutputStream
                );

                qrCodeImage = DefaultStreamedContent.builder()
                    .contentType("image/png")
                    .stream(() -> new ByteArrayInputStream(pngOutputStream.toByteArray()))
                    .build();
            } catch (Exception e) {
                addErrorMessage("Error", "Failed to generate QR code");
            }
        }
        return qrCodeImage;
    }
    
    // Method to refresh user data from the database
    public void refreshUserData() {
        if (currentUser != null && currentUser.getEmail() != null) {
            User freshUser = authService.findUserByEmail(currentUser.getEmail());
            if (freshUser != null) {
                this.currentUser = freshUser;
                this.firstName = freshUser.getFirstName();
                this.secondName = freshUser.getSecondName();
                this.email = freshUser.getEmail();
                this.balance = freshUser.getBalance();
                this.budget = freshUser.getBudget();
                this.piggyBank = freshUser.getPiggyBank();
            } else {
                addErrorMessage("Error", "Unable to refresh user data.");
            }
        }
    }

    // Helper methods for displaying messages
    private void addErrorMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    private void addInfoMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // Logout method
    public String logout() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
        if (session != null) {
            session.invalidate();
        }
        facesContext.addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Logged Out", "You have been successfully logged out."));
        return "index.xhtml?faces-redirect=true";
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getSecondName() { return secondName; }
    public void setSecondName(String secondName) { this.secondName = secondName; }
    
    public String getTwoFactorCode() { return twoFactorCode; }
    public void setTwoFactorCode(String twoFactorCode) { this.twoFactorCode = twoFactorCode; }
    
    public User getCurrentUser() { return currentUser; }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getMessageStyle() {
        return messageStyle;
    }
    
    public void setMessageStyle(String messageStyle) {
        this.messageStyle = messageStyle;
    }

    private String generateNewToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }
}
