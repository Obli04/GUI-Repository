package beans.services;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import beans.entities.User;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import jakarta.validation.ValidationException;

/**
 * AuthService provides authentication-related services such as login, email verification,
 * password reset, and two-factor authentication.
 * 
 * @author Davide Scaccia
 */
@ApplicationScoped
public class AuthService {
   @PersistenceContext(unitName = "e-walletPU")
   private EntityManager em;
   
   @Inject
   private EmailService emailService;
   
   @Inject
   private RateLimiterService rateLimiter;
   
   @jakarta.annotation.Resource
   private UserTransaction userTransaction;
   
   private GoogleAuthenticator gAuth;
   
   @PostConstruct
   public void init() {
       this.gAuth = new GoogleAuthenticator();
   }

   public void validatePassword(String password) throws jakarta.validation.ValidationException {
       if (password == null || password.trim().isEmpty()) {
           throw new jakarta.validation.ValidationException("Password cannot be empty");
       }
       
       if (password.length() < 8) {
           throw new jakarta.validation.ValidationException("Password must be at least 8 characters long");
       }
       
       if (!password.matches(".*[A-Z].*")) {
           throw new jakarta.validation.ValidationException("Password must contain at least one uppercase letter");
       }
       
       if (!password.matches(".*[a-z].*")) {
           throw new jakarta.validation.ValidationException("Password must contain at least one lowercase letter");
       }
       
       if (!password.matches(".*[0-9].*")) {
           throw new jakarta.validation.ValidationException("Password must contain at least one number");
       }
       
       if (!password.matches(".*[@#$%^&+=!].*")) {
           throw new jakarta.validation.ValidationException("Password must contain at least one special character (@#$%^&+=!)");
       }
   }

   @Transactional
   public RegistrationResult register(User user) throws Exception {
       try {
           // Debug database connection
           try {
               em.createNativeQuery("SELECT 1").getSingleResult();
           } catch (Exception e) {
               throw new Exception("Unable to connect to database: " + e.getMessage());
           }
           
           validatePassword(user.getPassword());
           if (user.getEmail() == null || user.getFirstName() == null || user.getSecondName() == null) {
               throw new jakarta.validation.ValidationException("All fields are required");
           }
           
           
           // Check for existing user
           try {
               User existingUser = findUserByEmail(user.getEmail());
               if (existingUser != null) {
                   throw new jakarta.validation.ValidationException("Email already registered");
               }
           } catch (ValidationException e) {
               if (!(e instanceof jakarta.validation.ValidationException)) {
                   throw new Exception("Database error while checking existing user");
               }
               throw e;
           }
           
           // Hash password
           try {
               user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
           } catch (Exception e) {
               throw new Exception("Error processing password");
           }
           
           // Set default values
           user.setIsVerified(false);
           user.setTwoFactorEnabled(false);
           user.setTwoFactorSecret(null);  // Don't generate secret at registration
           user.setVariableSymbol(generateVariableSymbol());
           
           // Generate verification token
           String token = UUID.randomUUID().toString();
           user.setVerificationToken(token);
           user.setTokenExpiry(LocalDateTime.now().plusHours(24));
           
           // Save user
           try {
               em.persist(user);
               em.flush();
           } catch (Exception e) {
               throw new Exception("Failed to save user to database: " + e.getMessage());
           }
           
           boolean emailSent = false;
           String emailStatus = "";
           // Send verification email
           try {
               emailService.sendVerificationEmail(user.getEmail(), token);
               emailSent = true;
               emailStatus = "Account created and verification email sent to: " + user.getEmail() + 
                           ". Please check your inbox and spam folder.";
           } catch (Exception e) {
               emailStatus = "Account created successfully, but we couldn't send the verification email. " +
                           "Please try requesting a new verification email after logging in.";
           }
           
           return new RegistrationResult(true, emailSent, emailStatus);
           
       } catch (Exception e) {
           throw e;
       }
   }

   /**
    * Retrieves the variable symbol for a user by their ID.
    *
    * @param id the ID of the user
    * @return the variable symbol of the user
    */
   @Transactional
   public String getVariableSymbol(Long id) {
       return em.createQuery("SELECT u.variable_symbol FROM User u WHERE u.id = :id", String.class)
                .setParameter("id", id)
                .getSingleResult();
   }

   /**
    * Verifies a user's email using a token.
    *
    * @param token the verification token
    * @return true if the email is successfully verified, false otherwise
    */
   @Transactional
   public boolean verifyEmail(String token) {
       User user = em.createQuery("SELECT u FROM User u WHERE u.verificationToken = :token", User.class)
                    .setParameter("token", token)
                    .getSingleResult();
                    
       if (user != null && LocalDateTime.now().isBefore(user.getTokenExpiry())) {
           user.setIsVerified(true);
           user.setVerificationToken(null);
           user.setTokenExpiry(null);
           em.merge(user);
           return true;
       }
       return false;
   }

   /**
    * Attempts to log in a user with email, password, and two-factor authentication code.
    *
    * @param email the user's email
    * @param password the user's password
    * @param twoFactorCode the two-factor authentication code
    * @return true if login is successful, false otherwise
    * @throws SecurityException if the account is locked or email is not verified
    */
   public boolean login(String email, String password, String twoFactorCode) {
       if (!rateLimiter.isAllowed(email)) {
           LocalDateTime lockoutEnd = rateLimiter.getLockoutEndTime(email);
           throw new SecurityException("Account temporarily locked. Try again after " + 
               lockoutEnd.toString());
       }

       try {
           User user = findUserByEmail(email);
           if (user != null && BCrypt.checkpw(password, user.getPassword())) {
               if (!user.getIsVerified()) {
                   throw new SecurityException("Email not verified");
               }
               
               if (user.isTwoFactorEnabled()) {
                   boolean valid = gAuth.authorize(user.getTwoFactorSecret(), 
                       Integer.parseInt(twoFactorCode));
                   if (!valid) {
                       rateLimiter.recordFailedAttempt(email);
                       return false;
                   }
               }
               rateLimiter.resetAttempts(email);
               return true;
           }
           rateLimiter.recordFailedAttempt(email);
           return false;
       } catch (NumberFormatException | SecurityException e) {
           rateLimiter.recordFailedAttempt(email);
           throw e;
       }
   }

   /**
    * Finds a user by their email.
    *
    * @param email the user's email
    * @return the User object if found, null otherwise
    */
   public User findUserByEmail(String email) {
       try {
           return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                   .setParameter("email", email)
                   .getSingleResult();
       } catch (Exception e) {
           return null;
       }
   }

   /**
    * Verifies a user's two-factor authentication code.
    *
    * @param user the user to verify
    * @param code the two-factor authentication code
    * @return true if the code is valid, false otherwise
    */
   @Transactional
   public boolean verify2FA(User user, String code) {
       try {
           if (user == null || code == null || user.getTwoFactorSecret() == null) {
               return false;
           }
           
           String cleanCode = code.trim().replaceAll("[^0-9]", "");
           
           if (cleanCode.length() != 6) {
               return false;
           }

           int codeInt = Integer.parseInt(cleanCode);
           boolean isValid = gAuth.authorize(user.getTwoFactorSecret(), codeInt);
                      return isValid;
       } catch (NumberFormatException e) {
           return false;
       }
   }

   /**
    * Requests a password reset for a user by email.
    *
    * @param email the user's email
    */
   @Transactional
   public void requestPasswordReset(String email) {
       User user = findUserByEmail(email);
       if (user != null) {
           String token = UUID.randomUUID().toString();
           user.setVerificationToken(token);
           user.setTokenExpiry(LocalDateTime.now().plusHours(1)); // 1 hour from now
           em.merge(user);
           try {
               emailService.sendPasswordResetEmail(email, token);
           } catch (Exception e) {
           }
       }
   }

   /**
    * Resets a user's password using a token and a new password.
    *
    * @param token the reset token
    * @param newPassword the new password
    * @return true if the password is successfully reset, false otherwise
    */
   @Transactional
   public boolean resetPassword(String token, String newPassword) {
       try {
           validatePassword(newPassword);
           User user = findUserByResetToken(token);

           if (user != null) {
               Timestamp currentTime = new Timestamp(System.currentTimeMillis());
               if (user.getTokenExpiry() != null && user.getTokenExpiry().isAfter(currentTime.toLocalDateTime())) {
                   user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                   user.setVerificationToken(null);
                   user.setTokenExpiry(null);
                   em.merge(user);
                   return true;
               }
           }
       } catch (ValidationException e) {
        return false;
       }
       return false;
   }

   /**
    * Resends a verification email to a user.
    *
    * @param email the user's email
    * @throws Exception if the user is not found or already verified
    */
   @Transactional
   public void resendVerificationEmail(String email) throws Exception {
       try {
           User user = findUserByEmail(email);
           if (user == null) {
               throw new Exception("No account found with this email address.");
           }
           
           if (user.getIsVerified()) {
               throw new Exception("This account is already verified.");
           }

           // Generate new verification token
           String token = UUID.randomUUID().toString();
           user.setVerificationToken(token);
           user.setTokenExpiry(LocalDateTime.now().plusHours(24));
           
           // Update user in database
           em.merge(user);
           em.flush();
           
           // Send verification email
           emailService.sendVerificationEmail(user.getEmail(), token);
           
       } catch (Exception e) {
           throw e;
       }
   }

   /**
    * Updates a user's information.
    *
    * @param user the user to update
    * @throws Exception if the user is not found or update fails
    */
   @Transactional
   public void updateUser(User user) throws Exception {
       try {
           // Validate user exists
           User existingUser = findUserByEmail(user.getEmail());
           if (existingUser == null) {
               throw new Exception("User not found");
           }

           // Update the user
           em.merge(user);
           em.flush();
       } catch (Exception e) {
           throw new Exception("Failed to update user: " + e.getMessage());
       }
   }

   /**
    * Updates a user's password.
    *
    * @param user the user whose password is to be updated
    * @param newPassword the new password
    * @throws Exception if the password validation or update fails
    */
   @Transactional
   public void updatePassword(User user, String newPassword) throws Exception {
       try {
           validatePassword(newPassword);
           user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
           em.merge(user);
           em.flush();
       } catch (ValidationException e) {
           throw new Exception("Failed to update password: " + e.getMessage());
       }
   }

   /**
    * Verifies a user's password.
    *
    * @param email the user's email
    * @param password the password to verify
    * @return true if the password is correct, false otherwise
    */
   public boolean verifyPassword(String email, String password) {
       try {
           User user = findUserByEmail(email);
           return user != null && BCrypt.checkpw(password, user.getPassword());
       } catch (Exception e) {
           return false;
       }
   }

   /**
    * Generates a new two-factor authentication secret.
    *
    * @return the new two-factor authentication secret
    */
   public String generateNewTwoFactorSecret() {
       GoogleAuthenticator gAuth = new GoogleAuthenticator();
       GoogleAuthenticatorKey key = gAuth.createCredentials();
       String secret = key.getKey();
       return secret;
   }

   /**
    * Finds a user by their reset token.
    *
    * @param token the reset token
    * @return the User object if found, null otherwise
    */
   public User findUserByResetToken(String token) {
       try {
           return em.createQuery("SELECT u FROM User u WHERE u.verificationToken = :token", User.class)
                    .setParameter("token", token)
                    .getSingleResult();
       } catch (Exception e) {
           return null;
       }
   }

   /**
    * Sends a password reset email to a user.
    *
    * @param email the user's email
    * @param token the reset token
    * @throws Exception if sending the email fails
    */
   public void sendPasswordResetEmail(String email, String token) throws Exception {
       emailService.sendPasswordResetEmail(email, token);
   }

   /**
    * Generates a unique variable symbol.
    *
    * @return a unique variable symbol
    */
   private String generateVariableSymbol() {
       String variableSymbol;
       boolean isUnique;

       do {
           // Generate a random number and format it to be 10 characters long
           long randomNumber = (long) (Math.random() * 1_000_000_000L);
           variableSymbol = String.format("%010d", randomNumber);

           // Check if the generated symbol already exists in the database
           isUnique = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.variableSymbol = :variableSymbol", Long.class)
                        .setParameter("variableSymbol", variableSymbol)
                        .getSingleResult() == 0;
       } while (!isUnique);

       return variableSymbol;
   }

   /**
    * Retrieves the latest balance for a user.
    *
    * @param userId the ID of the user
    * @return the latest balance of the user
    */
   @Transactional
   public double getLatestBalance(Long userId) {
       try {
           return em.createQuery("SELECT u.balance FROM User u WHERE u.id = :userId", Double.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
       } catch (Exception e) {
           return 0.0;
       }
   }
}
