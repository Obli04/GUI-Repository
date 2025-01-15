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
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import beans.deposit.DepositBean;
import beans.entities.Transaction;
import beans.entities.User;
import beans.services.AuthService;
import beans.services.RateLimiterService;
import beans.services.RegistrationResult;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ValidationException;

/**
 * Manages user-related operations such as login, 2FA, and password reset.
 * Handles user session data and interactions with the authentication service.
 * 
 * @author Davide Scaccia
 */
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
    private String iban;
    
    // Session data
    private User currentUser;
    private StreamedContent qrCodeImage;
    
    @Inject
    private AuthService authService;

    private String message;
    private String messageStyle;
    
    @Inject
    private DepositBean depositBean;

    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    private boolean showTwoFactorInput = false;
    private User tempUser;

    private String currentQRSecret;

    private String resetToken;

    @PersistenceContext(unitName = "e-walletPU")
    private EntityManager em;

    @Inject
    private SessionTimeoutConfig sessionTimeoutConfig;

    @Inject
    private RateLimiterService rateLimiterService;

    private String loginMessage;

    /**
     * Handles the registration process for the user.
     * 
     * @return the navigation outcome string, or null if registration fails
     */
    public String register() {
        try {
            
            if (!isValidEmail(email)) { //If the email is not valid show an error message
                addErrorMessage("Registration failed", "Please enter a valid email address");
                return null;
            }

            authService.validatePassword(password); //Validate the password
            User newUser = new User(); //Create a new user with the provided data
            newUser.setEmail(email.trim().toLowerCase());
            newUser.setPassword(password);
            newUser.setFirstName(firstName.trim());
            newUser.setSecondName(secondName.trim());
            
            //Check if user already exists
            if (authService.findUserByEmail(newUser.getEmail()) != null) { //If the user already exists show an error message
                addErrorMessage("Registration failed", "An account with this email already exists");
                return null;
            }
            
            try { //Try to register the user
                RegistrationResult result = authService.register(newUser); //Register the user
            
                //Store the result message in the session for display after redirect
            FacesContext context = FacesContext.getCurrentInstance();
                context.getExternalContext().getSessionMap().put("registrationMessage", result.getMessage()); //Store the message in the session
                context.getExternalContext().getSessionMap().put("emailSent", result.isEmailSent()); //Store the email sent status in the session
                return "login.xhtml?faces-redirect=true"; //Redirect to the login page
            } catch (Exception e) {
                addErrorMessage("Registration failed", "Failed to save user to database: " + e.getMessage()); //Show an error message
                return null;
            }
        } catch (ValidationException e) {
            addErrorMessage("Registration failed", e.getMessage()); //Show an error message
            return null;
        }
    }
 
    /**
     * Check registration message after redirecting
     */
    public void checkRegistrationMessage() {
        FacesContext context = FacesContext.getCurrentInstance(); //Get the current faces context
        message = (String) context.getExternalContext().getSessionMap().get("registrationMessage"); //Get the registration message from the session
        Boolean emailSent = (Boolean) context.getExternalContext().getSessionMap().get("emailSent"); //Get the email sent status from the session
        
        if (message != null) { //If we have a message
            FacesMessage.Severity severity = emailSent ? FacesMessage.SEVERITY_INFO : FacesMessage.SEVERITY_WARN; //Set the severity of the message
            context.addMessage(null, new FacesMessage(severity, "Registration Status", message)); //Add the message to the context
            
            //Clear the session attributes
            context.getExternalContext().getSessionMap().remove("registrationMessage");
            context.getExternalContext().getSessionMap().remove("emailSent");
        }
    }

    /**
     * 2FA verification
     * 
     * @return the navigation outcome string, or null if verification fails
     */
    public String verify2FA() {
        try {
            if (authService.verify2FA(currentUser, twoFactorCode)) return "dashboard.xhtml?faces-redirect=true"; //If the 2FA is verified redirect to the dashboard
            else addErrorMessage("Verification failed", "Invalid code"); //If the 2FA is not verified show an error message
        } catch (Exception e) {
            addErrorMessage("Error", e.getMessage()); //Show an error message
        }
        return null;
    }

    /**
     * Generate QR code for 2FA
     * 
     * @return the QR code image
     */
    public StreamedContent getQrCodeImage() {
        //Only generate QR if user has a secret and it's different from current QR
        if (currentUser != null && currentUser.getTwoFactorSecret() != null && (!currentUser.getTwoFactorSecret().equals(currentQRSecret) || qrCodeImage == null)){
            try {
                String otpauthURL = String.format( //Generate the OTP auth URL
                    "otpauth://totp/CashHive:%s?secret=%s&issuer=CashHive",
                    URLEncoder.encode(currentUser.getEmail(), "UTF-8"), //Encode the email
                    currentUser.getTwoFactorSecret() //Get the secret
                );
                QRCodeWriter qrCodeWriter = new QRCodeWriter(); //Create a QR code writer
                BitMatrix bitMatrix = qrCodeWriter.encode( //Encode the URL
                    otpauthURL, 
                    BarcodeFormat.QR_CODE, 
                    250, 
                    250
                );

                BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix); //Convert the bit matrix to an image
                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream(); //Create a byte array output stream
                ImageIO.write(qrImage, "PNG", pngOutputStream); //Write the image to the output stream

                qrCodeImage = DefaultStreamedContent.builder() //Create a default streamed content  
                    .contentType("image/png") //Set the content type
                    .stream(() -> new ByteArrayInputStream(pngOutputStream.toByteArray())) //Set the stream
                    .build();
                
                //Store the secret used for this QR code
                currentQRSecret = currentUser.getTwoFactorSecret();
            } catch (WriterException | IOException e) {
                addErrorMessage("Error", "Failed to generate QR code"); //Show an error message
            }
        }
        return qrCodeImage; //Return the QR code image
    }
    
    /*Penso che questa funzione non serva più: Refresh user data from the database
    public void refreshUserData() {
        if (currentUser != null && currentUser.getEmail() != null) { //If the current user is not null and the email is not null
            User freshUser = authService.findUserByEmail(currentUser.getEmail()); //Find the user by email
            if (freshUser != null) { //If the user is found refresh all the data
                this.currentUser = freshUser;
                this.firstName = freshUser.getFirstName();
                this.secondName = freshUser.getSecondName();
                this.balance = freshUser.getBalance();
                this.email = freshUser.getEmail();
                this.iban = freshUser.getIban();
                this.variableSymbol = freshUser.getVariableSymbol();    
            } else {
                addErrorMessage("Error", "Unable to refresh user data."); //Show an error message
            }
        }
    }*/

    /**
     * Helper methods for displaying messages
     */
    private void addErrorMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    /**
     * Add an info message
     */
    private void addInfoMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Logout method, it will clear the user data and invalidate the session
     * 
     * @return the navigation outcome string, or null if logout fails
     */
    public String logout() {
        FacesContext facesContext = FacesContext.getCurrentInstance(); //Get the current faces context
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false); //Get the current session
        
        //Clear user data
        this.currentUser = null;
        this.email = null;
        this.password = null;
        this.firstName = null;
        this.secondName = null;
        this.iban = null;
        
        if (session != null) session.invalidate(); //Invalidate the session
        facesContext.responseComplete(); //Complete the response
        
        return "index.xhtml?faces-redirect=true"; //Redirect to home page
    }

    /**
     * Generate a new token for password reset
     * 
     * @return the new token
     */
    private String generateNewToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Check if the email is valid
     * 
     * @param email the email to check
     * @return true if the email is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }

    /**
     * Update the user's profile
     * 
     * @return the navigation outcome string, or null if update fails
     */
    public String updateProfile() {
        try {
            User user = authService.findUserByEmail(currentUser.getEmail());
            user.setFirstName(firstName);
            user.setSecondName(secondName);
            user.setIban(iban);
            
            //If email is changed, verify it's not already in use
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

    /**
     * Change the user's password
     * 
     * @return the navigation outcome string, or null if change fails
     */
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

    /**
     * Enable 2FA
     * 
     * @return the navigation outcome string, or null if enable fails
     */
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

    /**
     * Disable 2FA
     * 
     * @return the navigation outcome string, or null if disable fails
     */
    public String disable2FA() {
        try {
            //If the 2FA code is not provided show an error message 
            if (twoFactorCode == null || twoFactorCode.trim().isEmpty()) {
                addErrorMessage("Error", "Please enter your 2FA code");
                return null;
            }

            //If the 2FA code is correct disable 2FA
            if (authService.verify2FA(currentUser, twoFactorCode)) {
                currentUser.setTwoFactorEnabled(false); //Disable 2FA
                currentUser.setTwoFactorSecret(null);  //Clear the secret
                authService.updateUser(currentUser); //Update the user
                
                //Clear form data
                twoFactorCode = null;
                qrCodeImage = null;
                
                addInfoMessage("Success", "Two-factor authentication disabled"); //Show a success message
                return null;
            } else {
                addErrorMessage("Error", "Invalid verification code"); //Show an error message
                return null;
            }
        } catch (Exception e) {
            addErrorMessage("Error", "Failed to disable 2FA"); //Show an error message
            return null;
        }
    }

    /**
     * Initiates the login process, handling 2FA if enabled.
     * 
     * @return navigation outcome string
     */
    public String initiateLogin() {
        if (!rateLimiterService.isAllowed(email)) {
            addErrorMessage("Login Failed", "Too many failed attempts. Please try again later.");
            return null;
        }

        try {
            User user = authService.findUserByEmail(email);

            if (user == null || !authService.verifyPassword(email, password)) {
                rateLimiterService.recordFailedAttempt(email);
                addErrorMessage("Login Failed", "Invalid email or password.");
                return null;
            }

            if (!user.getIsVerified()) {
                addErrorMessageWithLink("Login Failed", "User is not verified. Click here to request a new verification email.", "resendVerificationEmail");
                return null;
            }

            if (user.isTwoFactorEnabled()) {
                this.tempUser = user;
                this.showTwoFactorInput = true;
                return null;
            } else {
                completeLogin(user);
                sessionTimeoutConfig.configureSessionTimeout();
                rateLimiterService.resetAttempts(email);
                return "dashboard.xhtml?faces-redirect=true";
            }
        } catch (Exception e) {
            addErrorMessage("Login Error", e.getMessage());
            return null;
        }
    }

    /**
     * Resend verification email
     */
    public void resendVerificationEmail() {
        System.out.println("Attempting to resend verification email for: " + email);
        try {
            if (email == null || email.trim().isEmpty()) {
                addErrorMessage("Error", "No email address provided");
                return;
            }
            
            authService.resendVerificationEmail(email);
            loginMessage = "Verification email has been resent. Please check your inbox.";
            // Also add a regular message for better visibility
            addInfoMessage("Success", "Verification email has been resent");
        } catch (Exception e) {
            System.err.println("Failed to resend verification email: " + e.getMessage());
            loginMessage = "Failed to resend verification email: " + e.getMessage();
            addErrorMessage("Error", "Failed to resend verification email");
        }
    }

    private void addErrorMessageWithLink(String summary, String detail, String actionMethod) {
        String link = "<div class='error-message'>" + summary + " <a href='#' onclick=\"document.getElementById('hiddenForm:" + actionMethod + "').click(); return false;\">" + detail + "</a></div>";
        this.loginMessage = link;
    }

    /**
     * Completes the login process after 2FA verification.
     * 
     * @return the navigation outcome string, or null if login fails
     */
    public String completeTwoFactorLogin() {
        try { //Try to complete the login
            //If the user is not null and the 2FA code is correct
            if (tempUser != null && authService.verify2FA(tempUser, twoFactorCode)) {
                completeLogin(tempUser); //If the 2FA code is correct complete the login
                resetLoginForm(); //Reset the login form
                return "dashboard.xhtml?faces-redirect=true"; //Redirect to the dashboard
            } 
            else {
                addErrorMessage("Verification Failed", "Invalid 2FA code"); //Show an error message
                return null;
            }
        } catch (Exception e) {
            addErrorMessage("Login Error", e.getMessage()); //Show an error message
            return null;
        }
    }

    /**
     * Resets the login form data.
     */
    private void resetLoginForm() {
        this.showTwoFactorInput = false;
        this.tempUser = null;
        this.twoFactorCode = null;
        this.password = null;
    }

    /**
     * Completes the login by setting user session data.
     * 
     * @param user the user to log in
     */
    private void completeLogin(User user) {
        //Set the user data
        currentUser = user;
        this.firstName = user.getFirstName();
        this.secondName = user.getSecondName();
        this.email = user.getEmail();
        this.iban = user.getIban();
        
        //Store in session
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(true);
        session.setAttribute("userBean", this);

        depositBean.initializeDeposit(); //Initialize the deposit information
    }

    public String initiate2FASetup() {
        try {
            //Generate new secret
            String secret = authService.generateNewTwoFactorSecret();
            
            //Update the user with the new secret
            currentUser.setTwoFactorSecret(secret);
            currentUser.setTwoFactorEnabled(false);
            authService.updateUser(currentUser);
            
            //Clear the existing code field and QR code
            twoFactorCode = null;
            qrCodeImage = null;
            currentQRSecret = null; //Reset the QR secret tracker
            
            addInfoMessage("2FA Setup", "Scan the QR code with your authenticator app"); //Show a success message   
            return null;
        } catch (Exception e) {
            addErrorMessage("Error", "Failed to initiate 2FA setup"); //Show an error message
            return null;
        }
    }

    /**
     * Request a password reset
     * 
     * @return the navigation outcome string, or null if reset fails
     */
    public String requestPasswordReset() {
        try {
            User user = authService.findUserByEmail(email); //Find the user by email
            if (user == null) {
                addErrorMessage("Reset Failed", "No account found with this email address."); //Show an error message
                return null;
            }

            //Generate a reset token
            String token = generateNewToken();
            user.setVerificationToken(token);
            user.setTokenExpiry(LocalDateTime.now().plusHours(1)); //1 hour from now
            authService.updateUser(user);

            //Send reset email
            authService.sendPasswordResetEmail(user.getEmail(), token);

            //Store success message in session
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("resetMessage", 
                "If an account exists with this email, you will receive password reset instructions.");

            return "login?faces-redirect=true";
        } catch (Exception e) {
            addErrorMessage("Reset Failed", "An error occurred while processing your request.");
            return null;
        }
    }

    /**
     * Reset the user's password
     * 
     * @return the navigation outcome string, or null if reset fails
     */
    public String resetPassword() {
        try {
            //If the reset token is null or empty show an error message 
            if (resetToken == null || resetToken.trim().isEmpty()) {
                addErrorMessage("Reset Failed", "Invalid reset token"); //Show an error message
                return null;
            }

            //If the new password is null or empty show an error message
            if (newPassword == null || newPassword.trim().isEmpty()) {
                addErrorMessage("Reset Failed", "Password cannot be empty"); //Show an error message
                return null;
            }

            //If the new password and the confirm password do not match show an error message
            if (!newPassword.equals(confirmPassword)) {
                addErrorMessage("Reset Failed", "Passwords do not match"); //Show an error message
                return null;
            }

            User user = authService.findUserByResetToken(resetToken); //Find the user by the reset token
            if (user == null) {
                addErrorMessage("Reset Failed", "Invalid or expired reset token"); //Show an error message
                return null;
            }

            //Check if the token is expired
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            if (user.getTokenExpiry() == null || 
                user.getTokenExpiry().isBefore(currentTime.toLocalDateTime())) {
                addErrorMessage("Reset Failed", "Reset token has expired"); //Show an error message
                return null;
            }

            //Update password and clear reset token
            authService.updatePassword(user, newPassword);
            user.setVerificationToken(null);
            user.setTokenExpiry(null);
            authService.updateUser(user);

            //Store success message in session
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("resetMessage", 
                "Your password has been reset successfully. Please log in with your new password."); //Show a success message

            return "login?faces-redirect=true";
        } catch (Exception e) {
            addErrorMessage("Reset Failed", e.getMessage());
            return null;
        }
    }

    /**
     * Check and display messages after redirect
     */
    public void checkResetMessage() {
        //Get the current faces context
        FacesContext context = FacesContext.getCurrentInstance();
        message = (String) context.getExternalContext().getSessionMap().get("resetMessage");
        //If the message is not null add it to the context  
        if (message != null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "", message));
            context.getExternalContext().getSessionMap().remove("resetMessage");
        }
    }

    /**
     * Get the user's transactions
     * 
     * @param userId the user's id
     * @return the user's transactions
     */
    public List<Transaction> getUserTransactions(Long userId) {
        return em.createQuery("SELECT t FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId OR (t.sender IS NULL AND t.receiver.id = :userId)) ORDER BY t.transactionDate DESC", Transaction.class)
                 .setParameter("userId", userId)
                 .getResultList();
    }


    /**
     * Refreshes the user's balance from the database.
     */
    public void refreshBalance() {
        currentUser = getCurrentUser();
        if (currentUser != null) {
            double latestBalance = authService.getLatestBalance(currentUser.getId());
            currentUser.setBalance(latestBalance);
        }
    }
    

    /**
     * Getters and Setters
     */
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
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getMessageStyle() { return messageStyle; }
    public void setMessageStyle(String messageStyle) { this.messageStyle = messageStyle; }  
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public boolean isShowTwoFactorInput() { return showTwoFactorInput; }
    public void setShowTwoFactorInput(boolean showTwoFactorInput) { this.showTwoFactorInput = showTwoFactorInput; }
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public String getLoginMessage() {
        return loginMessage;
    }
    public void clearLoginMessage() {
        this.loginMessage = null;
    }
}
