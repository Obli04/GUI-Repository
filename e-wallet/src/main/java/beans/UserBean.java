package beans;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import beans.deposit.DepositBean;
import beans.entities.Transaction;
import beans.entities.User;
import beans.services.AuthService;
import beans.services.RegistrationResult;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private String iban;
    private String variableSymbol;
    
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

    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    private boolean showTwoFactorInput = false;
    private User tempUser; // Store user temporarily during 2FA

    private String currentQRSecret; // Add this field

    private String resetToken;

    @PersistenceContext(unitName = "e-walletPU")
    private EntityManager em;

    // Login method
    public String login() {
        try {
            System.out.println("Login attempt for email: " + email);
            User user = authService.findUserByEmail(email);
            System.out.println("User found: " + (user != null));
            if (user != null) {
                System.out.println("2FA enabled: " + user.isTwoFactorEnabled());
            }
            // First, find the user to check if 2FA is enabled
            if (user == null) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login Failed", "Invalid email or password."));
                return null;
            }

            // Check if 2FA is enabled for this user
            boolean isAuthenticated;
            if (user.isTwoFactorEnabled()) {
                isAuthenticated = authService.login(email, password, twoFactorCode);
            } else {
                isAuthenticated = authService.login(email, password, null);
            }

            if (isAuthenticated) {
                // Retrieve the authenticated user
                currentUser = user;
                
                // Set user data
                this.firstName = currentUser.getFirstName();
                this.secondName = currentUser.getSecondName();
                this.email = currentUser.getEmail();
                this.balance = currentUser.getBalance();
                this.budget = currentUser.getBudget();
                this.piggyBank = currentUser.getPiggyBank();
                this.iban = currentUser.getIban();
                this.variableSymbol = currentUser.getVariableSymbol();
                // Store in session
                FacesContext facesContext = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(true);
                session.setAttribute("userBean", this);
                
                // Initialize deposit information
                depositBean.initializeDeposit();
                
                return "dashboard.xhtml?faces-redirect=true";
            } else {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login Failed", 
                        user.isTwoFactorEnabled() ? "Invalid credentials or 2FA code." : "Invalid email or password."));
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
            
            // Check if user already exists
            if (authService.findUserByEmail(newUser.getEmail()) != null) {
                addErrorMessage("Registration failed", "An account with this email already exists");
                return null;
            }
            
            try {
                RegistrationResult result = authService.register(newUser);
                
                // Store the result message in the session for display after redirect
                FacesContext context = FacesContext.getCurrentInstance();
                context.getExternalContext().getSessionMap().put("registrationMessage", result.getMessage());
                context.getExternalContext().getSessionMap().put("emailSent", result.isEmailSent());
                
                System.out.println("Registration completed - Email sent: " + result.isEmailSent());
                System.out.println("Message: " + result.getMessage());
                System.out.println("=== End Registration Process ===\n");
                
                return "login.xhtml?faces-redirect=true";
            } catch (Exception e) {
                System.err.println("Failed to save user: " + e.getMessage());
                e.printStackTrace();
                addErrorMessage("Registration failed", "Failed to save user to database: " + e.getMessage());
                return null;
            }
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
        // Only generate QR if user has a secret and it's different from current QR
        if (currentUser != null && 
            currentUser.getTwoFactorSecret() != null && 
            (!currentUser.getTwoFactorSecret().equals(currentQRSecret) || qrCodeImage == null)) {
            
            try {
                String otpauthURL = String.format(
                    "otpauth://totp/CashHive:%s?secret=%s&issuer=CashHive",
                    URLEncoder.encode(currentUser.getEmail(), "UTF-8"),
                    currentUser.getTwoFactorSecret()
                );

                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(
                    otpauthURL, 
                    BarcodeFormat.QR_CODE, 
                    250, 
                    250
                );

                BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                ImageIO.write(qrImage, "PNG", pngOutputStream);

                qrCodeImage = DefaultStreamedContent.builder()
                    .contentType("image/png")
                    .stream(() -> new ByteArrayInputStream(pngOutputStream.toByteArray()))
                    .build();
                
                // Store the secret used for this QR code
                currentQRSecret = currentUser.getTwoFactorSecret();
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
                this.balance = authService.getLatestBalance(freshUser.getId());
                this.budget = freshUser.getBudget();
                this.piggyBank = freshUser.getPiggyBank();
                this.iban = freshUser.getIban();
                this.variableSymbol = freshUser.getVariableSymbol();
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
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
            
            // Clear user data
            this.currentUser = null;
            this.email = null;
            this.password = null;
            this.firstName = null;
            this.secondName = null;
            this.balance = null;
            this.budget = null;
            this.piggyBank = null;
            this.iban = null;
            this.variableSymbol = null;
            
            // Invalidate session
            if (session != null) {
                session.invalidate();
            }
            
            // Perform redirect
            facesContext.getExternalContext().redirect(
                facesContext.getExternalContext().getRequestContextPath() + "/index.xhtml");
            facesContext.responseComplete();
            
            return null;
        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Logout Error", "An error occurred during logout."));
            return null;
        }
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

    public String updateProfile() {
        try {
            User user = authService.findUserByEmail(currentUser.getEmail());
            user.setFirstName(firstName);
            user.setSecondName(secondName);
            user.setIban(iban);
            
            // If email is changed, verify it's not already in use
            if (!user.getEmail().equals(email)) {
                if (authService.findUserByEmail(email) != null) {
                    addErrorMessage("Update Failed", "Email already in use");
                    return null;
                }
                user.setEmail(email);
            }
            
            authService.updateUser(user);
            currentUser = user;
            addInfoMessage("Success", "Profile updated successfully");
            return null;
        } catch (Exception e) {
            addErrorMessage("Update Failed", e.getMessage());
            return null;
        }
    }

    public String changePassword() {
        try {
            if (!newPassword.equals(confirmPassword)) {
                addErrorMessage("Password Change Failed", "New passwords do not match");
                return null;
            }

            if (authService.verifyPassword(currentUser.getEmail(), currentPassword)) {
                authService.updatePassword(currentUser, newPassword);
                addInfoMessage("Success", "Password changed successfully");
                // Clear password fields
                currentPassword = null;
                newPassword = null;
                confirmPassword = null;
                return null;
            } else {
                addErrorMessage("Password Change Failed", "Current password is incorrect");
                return null;
            }
        } catch (Exception e) {
            addErrorMessage("Password Change Failed", e.getMessage());
            return null;
        }
    }

    public String enable2FA() {
        try {
            System.out.println("Attempting 2FA setup with:");
            System.out.println("User: " + currentUser.getEmail());
            System.out.println("Secret: " + currentUser.getTwoFactorSecret());
            System.out.println("Code: " + twoFactorCode);

            if (authService.verify2FA(currentUser, twoFactorCode)) {
                currentUser.setTwoFactorEnabled(true);
                authService.updateUser(currentUser);
                addInfoMessage("Success", "Two-factor authentication enabled");
                return null;
            } else {
                addErrorMessage("Error", "Invalid verification code");
                return null;
            }
        } catch (Exception e) {
            addErrorMessage("Error", "Failed to enable 2FA");
            return null;
        }
    }

    public String disable2FA() {
        try {
            if (twoFactorCode == null || twoFactorCode.trim().isEmpty()) {
                addErrorMessage("Error", "Please enter your 2FA code");
                return null;
            }

            if (authService.verify2FA(currentUser, twoFactorCode)) {
                currentUser.setTwoFactorEnabled(false);
                currentUser.setTwoFactorSecret(null);  // Clear the secret
                authService.updateUser(currentUser);
                
                // Clear form data
                twoFactorCode = null;
                qrCodeImage = null;
                
                addInfoMessage("Success", "Two-factor authentication disabled");
                return null;
            } else {
                addErrorMessage("Error", "Invalid verification code");
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error disabling 2FA: " + e.getMessage());
            e.printStackTrace();
            addErrorMessage("Error", "Failed to disable 2FA");
            return null;
        }
    }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public boolean isShowTwoFactorInput() {
        return showTwoFactorInput;
    }

    // Split the login process into two steps
    public String initiateLogin() {
        try {
            User user = authService.findUserByEmail(email);
            if (user == null || !authService.verifyPassword(email, password)) {
                addErrorMessage("Login Failed", "Invalid email or password.");
                return null;
            }

            if (!user.getIsVerified()) {
                addErrorMessage("Login Failed", "Email not verified.");
                return null;
            }

            if (user.isTwoFactorEnabled()) {
                // Store user temporarily and show 2FA input
                this.tempUser = user;
                this.showTwoFactorInput = true;
                return null;
            } else {
                // Complete login immediately if 2FA is not enabled
                completeLogin(user);
                return "dashboard.xhtml?faces-redirect=true";
            }
        } catch (Exception e) {
            addErrorMessage("Login Error", "An unexpected error occurred.");
            return null;
        }
    }

    public String completeTwoFactorLogin() {
        try {
            if (tempUser != null && authService.verify2FA(tempUser, twoFactorCode)) {
                completeLogin(tempUser);
                resetLoginForm();
                return "dashboard.xhtml?faces-redirect=true";
            }
            addErrorMessage("Verification Failed", "Invalid 2FA code");
            return null;
        } catch (Exception e) {
            addErrorMessage("Login Error", e.getMessage());
            return null;
        }
    }

    public String resetLogin() {
        resetLoginForm();
        return null;
    }

    private void resetLoginForm() {
        this.showTwoFactorInput = false;
        this.tempUser = null;
        this.twoFactorCode = null;
        this.password = null;
    }

    private void completeLogin(User user) {
        currentUser = user;
        this.firstName = user.getFirstName();
        this.secondName = user.getSecondName();
        this.email = user.getEmail();
        this.balance = user.getBalance();
        this.budget = user.getBudget();
        this.piggyBank = user.getPiggyBank();
        
        // Store in session
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(true);
        session.setAttribute("userBean", this);
        
        // Initialize deposit information
        depositBean.initializeDeposit();
    }

    public String initiate2FASetup() {
        try {
            // Generate new secret
            String secret = authService.generateNewTwoFactorSecret();
            
            // Update user with new secret
            currentUser.setTwoFactorSecret(secret);
            currentUser.setTwoFactorEnabled(false);
            authService.updateUser(currentUser);
            
            // Clear existing code field and QR code
            twoFactorCode = null;
            qrCodeImage = null;
            currentQRSecret = null; // Reset the QR secret tracker
            
            addInfoMessage("2FA Setup", "Scan the QR code with your authenticator app");
            return null;
        } catch (Exception e) {
            addErrorMessage("Error", "Failed to initiate 2FA setup");
            return null;
        }
    }

    public String requestPasswordReset() {
        try {
            User user = authService.findUserByEmail(email);
            if (user == null) {
                addErrorMessage("Reset Failed", "No account found with this email address.");
                return null;
            }

            // Generate a reset token
            String token = generateNewToken();
            user.setVerificationToken(token);
            user.setTokenExpiry(LocalDateTime.now().plusHours(1)); // 1 hour from now
            authService.updateUser(user);

            // Send reset email
            authService.sendPasswordResetEmail(user.getEmail(), token);

            // Store success message in session
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("resetMessage", 
                "If an account exists with this email, you will receive password reset instructions.");

            return "login?faces-redirect=true";
        } catch (Exception e) {
            addErrorMessage("Reset Failed", "An error occurred while processing your request.");
            return null;
        }
    }

    public String resetPassword() {
        try {
            if (resetToken == null || resetToken.trim().isEmpty()) {
                addErrorMessage("Reset Failed", "Invalid reset token");
                return null;
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                addErrorMessage("Reset Failed", "Password cannot be empty");
                return null;
            }

            if (!newPassword.equals(confirmPassword)) {
                addErrorMessage("Reset Failed", "Passwords do not match");
                return null;
            }

            User user = authService.findUserByResetToken(resetToken);
            if (user == null) {
                addErrorMessage("Reset Failed", "Invalid or expired reset token");
                return null;
            }

            // Check if token is expired
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            if (user.getTokenExpiry() == null || 
                user.getTokenExpiry().isBefore(currentTime.toLocalDateTime())) {
                addErrorMessage("Reset Failed", "Reset token has expired");
                return null;
            }

            // Update password and clear reset token
            authService.updatePassword(user, newPassword);
            user.setVerificationToken(null);
            user.setTokenExpiry(null);
            authService.updateUser(user);

            // Store success message in session
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("resetMessage", 
                "Your password has been reset successfully. Please log in with your new password.");

            return "login?faces-redirect=true";
        } catch (Exception e) {
            addErrorMessage("Reset Failed", e.getMessage());
            return null;
        }
    }

    // Method to check and display messages after redirect
    public void checkResetMessage() {
        FacesContext context = FacesContext.getCurrentInstance();
        String message = (String) context.getExternalContext().getSessionMap().get("resetMessage");
        if (message != null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "", message));
            context.getExternalContext().getSessionMap().remove("resetMessage");
        }
    }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public List<Transaction> getUserTransactions(Long userId) {
        return em.createQuery("SELECT t FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId OR (t.sender IS NULL AND t.receiver.id = :userId)) ORDER BY t.transactionDate DESC", Transaction.class)
                 .setParameter("userId", userId)
                 .getResultList();
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public void refreshBalance() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            double latestBalance = authService.getLatestBalance(currentUser.getId());
            currentUser.setBalance(latestBalance);
        }
    }
}
