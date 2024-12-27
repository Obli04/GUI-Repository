package beans.services;
import java.time.LocalDateTime;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

import com.warrenstrange.googleauth.GoogleAuthenticator;

import beans.entities.User;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;

@ApplicationScoped
public class AuthService {
   @PersistenceContext(unitName = "e-walletPU")
   private EntityManager em;
   
   @Inject
   private EmailService emailService;
   
   private GoogleAuthenticator gAuth;
   
   @Inject
   private RateLimiterService rateLimiter;
   
   @jakarta.annotation.Resource
   private UserTransaction userTransaction;
   
   @PostConstruct
   public void init() {
       gAuth = new GoogleAuthenticator();
       System.out.println("AuthService initialized. EntityManager: " + (em != null ? "present" : "null"));
   }

   private static final String PASSWORD_PATTERN = 
       "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

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
       System.out.println("AuthService: Starting registration for user: " + user.getEmail());
       try {
           // Debug database connection
           try {
               em.createNativeQuery("SELECT 1").getSingleResult();
               System.out.println("AuthService: Database connection test successful");
           } catch (Exception e) {
               System.err.println("AuthService ERROR: Database connection test failed: " + e.getMessage());
               e.printStackTrace();
               throw new Exception("Unable to connect to database: " + e.getMessage());
           }

           // Validate inputs
           if (user == null) {
               throw new jakarta.validation.ValidationException("User data cannot be null");
           }
           
           validatePassword(user.getPassword());
           
           if (user.getEmail() == null || user.getFirstName() == null || user.getSecondName() == null) {
               throw new jakarta.validation.ValidationException("All fields are required");
           }
           
           System.out.println("Starting registration for email: " + user.getEmail());
           
           // Check for existing user
           try {
               User existingUser = findUserByEmail(user.getEmail());
               if (existingUser != null) {
                   throw new jakarta.validation.ValidationException("Email already registered");
               }
           } catch (Exception e) {
               System.err.println("Error checking existing user: " + e.getMessage());
               if (!(e instanceof jakarta.validation.ValidationException)) {
                   throw new Exception("Database error while checking existing user");
               }
               throw e;
           }
           
           // Hash password
           try {
               user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
           } catch (Exception e) {
               System.err.println("Error hashing password: " + e.getMessage());
               throw new Exception("Error processing password");
           }
           
           // Set default values
           user.setIsVerified(false);
           user.setTwoFactorEnabled(false);
           
           // Generate verification token
           String token = UUID.randomUUID().toString();
           user.setVerificationToken(token);
           user.setTokenExpiry(LocalDateTime.now().plusHours(24));
           
           // Initialize GoogleAuthenticator if needed
           if (gAuth == null) {
               gAuth = new GoogleAuthenticator();
           }
           
           // Generate 2FA secret
           try {
               String secret = gAuth.createCredentials().getKey();
               user.setTwoFactorSecret(secret);
           } catch (Exception e) {
               System.err.println("Error generating 2FA secret: " + e.getMessage());
               throw new Exception("Error setting up 2FA");
           }
           
           // Save user
           try {
               System.out.println("Attempting to persist user");
               em.persist(user);
               em.flush();
               System.out.println("User persisted successfully");
           } catch (Exception e) {
               System.err.println("Error persisting user: " + e.getMessage());
               e.printStackTrace();
               throw new Exception("Failed to save user to database: " + e.getMessage());
           }
           
           boolean emailSent = false;
           String emailStatus = "";
           
           // Send verification email
           try {
               System.out.println("AuthService: Attempting to send verification email...");
               emailService.sendVerificationEmail(user.getEmail(), token);
               emailSent = true;
               emailStatus = "Account created and verification email sent to: " + user.getEmail() + 
                           ". Please check your inbox and spam folder.";
               System.out.println("AuthService: " + emailStatus);
           } catch (Exception e) {
               emailStatus = "Account created successfully, but we couldn't send the verification email. " +
                           "Please try requesting a new verification email after logging in.";
               System.err.println("AuthService ERROR: Failed to send verification email: " + e.getMessage());
               e.printStackTrace();
           }
           
           return new RegistrationResult(true, emailSent, emailStatus);
           
       } catch (Exception e) {
           System.err.println("AuthService ERROR: Registration failed: " + e.getMessage());
           e.printStackTrace();
           throw e;
       }
   }

   private boolean isEntityManagerActive() {
       try {
           // Try to access the EntityManager
           em.isOpen();
           return true;
       } catch (Exception e) {
           System.err.println("EntityManager check failed: " + e.getMessage());
           return false;
       }
   }

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
       } catch (Exception e) {
           rateLimiter.recordFailedAttempt(email);
           throw e;
       }
   }

   public User findUserByEmail(String email) {
       try {
           return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                   .setParameter("email", email)
                   .getSingleResult();
       } catch (Exception e) {
           return null;
       }
   }

   @Transactional
   public boolean verify2FA(User user, String code) {
       try {
           if (user != null && code != null) {
               boolean isValid = gAuth.authorize(user.getTwoFactorSecret(), Integer.parseInt(code));
               if (isValid) {
                   user.setTwoFactorEnabled(true);
                   em.merge(user);
               }
               return isValid;
           }
           return false;
       } catch (NumberFormatException e) {
           return false;
       }
   }

   @Transactional
   public void requestPasswordReset(String email) {
       User user = findUserByEmail(email);
       if (user != null) {
           String token = UUID.randomUUID().toString();
           user.setVerificationToken(token);
           user.setTokenExpiry(LocalDateTime.now().plusHours(1));
           em.merge(user);
           try {
               emailService.sendPasswordResetEmail(email, token);
           } catch (Exception e) {
               System.err.println("Failed to send password reset email: " + e.getMessage());
           }
       }
   }

   @Transactional
   public boolean resetPassword(String token, String newPassword) {
       try {
           validatePassword(newPassword);
           User user = em.createQuery("SELECT u FROM User u WHERE u.verificationToken = :token", User.class)
                        .setParameter("token", token)
                        .getSingleResult();

           if (user != null && LocalDateTime.now().isBefore(user.getTokenExpiry())) {
               user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
               user.setVerificationToken(null);
               user.setTokenExpiry(null);
               em.merge(user);
               return true;
           }
       } catch (Exception e) {
           // Log error
       }
       return false;
   }

   public void resendVerificationEmail(User user) throws Exception {
       // Generate new verification token
       String token = UUID.randomUUID().toString();
       user.setVerificationToken(token);
       user.setTokenExpiry(LocalDateTime.now().plusHours(24));
       
       try {
           em.merge(user);
           em.flush();
           emailService.sendVerificationEmail(user.getEmail(), token);
           System.out.println("Verification email resent successfully to: " + user.getEmail());
       } catch (Exception e) {
           System.err.println("Failed to resend verification email: " + e.getMessage());
           e.printStackTrace();
           throw new Exception("Failed to resend verification email: " + e.getMessage());
       }
   }
}
