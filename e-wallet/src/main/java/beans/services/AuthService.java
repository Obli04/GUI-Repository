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
import jakarta.validation.ValidationException;

/**
 * AuthService provides authentication-related services such as login, email verification,
 * password reset, and two-factor authentication.
 * 
 * @author Davide Scaccia - xscaccd00
 */
@ApplicationScoped
public class AuthService {
    
   @PersistenceContext
   private EntityManager em;
   
   @Inject
   private EmailService emailService;
   
   @Inject
   private RateLimiterService rateLimiter;
   
   
   private GoogleAuthenticator gAuth;

   private int twoFACode;
   /**
    * Initialize the GoogleAuthenticator
    */
    @PostConstruct
   public void init() {
       this.gAuth = new GoogleAuthenticator();
   }

   /**
    * Validate the password
    * @param password the password to validate
    * @throws jakarta.validation.ValidationException if the password is not valid
    */
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

   /**
    * Register a new user
    * @param user the user to register
    * @return the registration result
    * @throws Exception if the registration fails
    */
   @Transactional
   public RegistrationResult register(User user) throws Exception {
        try {
            validatePassword(user.getPassword()); //Validate the password
            if (user.getEmail() == null || user.getFirstName() == null || user.getSecondName() == null) { //Verify that all fields have been entered
               throw new jakarta.validation.ValidationException("All fields are required");
            }
           
            //Check for existing user
           try {
               User existingUser = findUserByEmail(user.getEmail()); //Search if the user's entered email is alredy in the database
               if (existingUser != null) throw new jakarta.validation.ValidationException("Email already registered"); //If we found a user throw an exception.
           } catch (ValidationException e) {
               if (!(e instanceof jakarta.validation.ValidationException)) throw new Exception("Database error while checking existing user"); //Database error
               throw e;
           }
           
           //Hash password
           try {
               user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt())); //Set the hashed password
           } catch (Exception e) {
               throw new Exception("Error processing password"); //Error with Encryption
           }
           
           //Set default values of user's account
           user.setIsVerified(false); //Is not verified (email verification)
           user.setTwoFactorEnabled(false); //2FA is not enabled
           user.setTwoFactorSecret(null);  //2FA secret will not be generated until you press on Setup 2FA
           user.setVariableSymbol(generateVariableSymbol()); //Generate the variable symbol for the user
           
           //Generate verification token
           String token = UUID.randomUUID().toString(); //Generate a verification token to verify the user's email
           user.setVerificationToken(token); //Set the verification token
           user.setTokenExpiry(LocalDateTime.now().plusHours(24)); //Set expiration date in 1 day
           
           try {
               em.persist(user); //Save the new user object to the database
               em.flush(); //Force any pending database operations to be executed immediately
           } catch (Exception e) {
               throw new Exception("Failed to save user to database: " + e.getMessage()); //If failed to save throw an exception
           }
           
           RegistrationResult result = new RegistrationResult(); //Create a new RegistrationResult object
           result.setSuccess(true); //Set success to true (user has been registered into database already either way)
           try {
               emailService.sendVerificationEmail(user.getEmail(), token); //Send the verification email with the generated token
               result.setEmailSent(true); //Set email sent to true
               result.setMessage("Account created and verification email sent to: " + user.getEmail() + //Set email status to valid
                           ". Please check your inbox and spam folder.");
           } catch (Exception e) {
               result.setEmailSent(false); //Set email sent to false
               result.setMessage("Account created successfully, but we couldn't send the verification email. " + //If problem while sending email then ask the user to resend the verification email during login
                           "Please try requesting a new verification email after logging in.");
           }
           return result; //Return the registration result
           
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
       return em.createQuery("SELECT u.variable_symbol FROM User u WHERE u.id = :id", String.class) //Select the variable symbol from the db and return the string.
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
       User user = em.createQuery("SELECT u FROM User u WHERE u.verificationToken = :token", User.class) //Select the user by the verification token
                    .setParameter("token", token)
                    .getSingleResult();
                    
       if (user != null && LocalDateTime.now().isBefore(user.getTokenExpiry())) { //If we find the user and the token is not expired yet
           user.setIsVerified(true); //Verify the user
           user.setVerificationToken(null); //Remove the verification token (and token expiry)
           user.setTokenExpiry(null);
           em.merge(user); //Merge the changes with the user
           em.flush(); //Execute changes
           return true; //We verified the email!
       }
       return false; //We didn't verify the email
   }

   /**
    * Attempts to log in a user with email, password, and two-factor authentication code.
    *
    * @param email the user's email
    * @param password the user's password
    * @param twoFactorCode the two-factor authentication code
    * @return true if login is successful, false otherwise
    * @throws SecurityException if the account is locked or email is not verified
    * @throws NumberFormatException if the two-factor code is not a number
    */
   public boolean login(String email, String password, String twoFactorCode) {
       if (!rateLimiter.isAllowed(email)) { //If the email has not been locked yet
           LocalDateTime lockoutEnd = rateLimiter.getLockoutEndTime(email); //Calculate the lockout end time
           throw new SecurityException("Account temporarily locked. Try again after " + 
               lockoutEnd.toString()); //Throw an exception with the lockout end time
       }

       try {
           User user = findUserByEmail(email); //Find the user by the input email
           if (user != null && BCrypt.checkpw(password, user.getPassword())) { //If the user exists and the password is correct
               if (!user.getIsVerified()) { //If the user is not verified
                   throw new SecurityException("Email not verified"); //Throw an exception
               }
               
               if (user.isTwoFactorEnabled()) { //If user has 2FA enabled
                   boolean valid = gAuth.authorize(user.getTwoFactorSecret(), Integer.parseInt(twoFactorCode)); //Verify the 2FA code
                   if (!valid) { //If 2FA code is invalid
                       rateLimiter.recordFailedAttempt(email); //Record the failed attempt
                       return false; //Return false
                   }
               }
               rateLimiter.resetAttempts(email); //Reset the attempts
               return true; //Valid login!
           }
           rateLimiter.recordFailedAttempt(email); //Record the failed attempt
           return false; //Invalid login
       } catch (NumberFormatException | SecurityException e) {
           throw e; //Throw the exception
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
           return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class) //Select the user by email
                   .setParameter("email", email)
                   .getSingleResult(); //Return the user
       } catch (Exception e) {
           return null; //If no user is found return null
       }
   }

   /**
    * Verifies a user's two-factor authentication code.
    *
    * @param user the user to verify
    * @param code the two-factor authentication code
    * @return true if the code is valid, false otherwise
    * @throws NumberFormatException if the two-factor code is not a number
    */
   @Transactional
   public boolean verify2FA(User user, String code) {
       try {
           if (user == null || code == null || user.getTwoFactorSecret() == null) { //If user is null or code is null or user has no 2FA secret
               return false; 
           }
           
           if (code.length() != 6) { //If the code is not 6 digits long
               return false;
           }

           int codeInt = Integer.parseInt(code); //Convert the code to an integer
           boolean isValid = gAuth.authorize(user.getTwoFactorSecret(), codeInt) || codeInt == twoFACode; //Verify the 2FA code
           return isValid; //Return the result
       } catch (NumberFormatException e) {
           return false;
       }
   }

   /**
    * Sends a 2FA code to the specified email address.
    *
    * @param email the recipient's email address
    * @param code the 2FA code to be included in the email
    * @throws Exception if an error occurs while sending the email
    */
   public void send2FACodeEmail(String email, String code) throws Exception {
       twoFACode = Integer.parseInt(code);
       emailService.send2FACodeEmail(email, code);
   }

   /**
    * Requests a password reset for a user by email.
    *
    * @param email the user's email
    */
   @Transactional
   public void requestPasswordReset(String email) throws Exception {
       User user = findUserByEmail(email); //Find the user by the input email
       if (user != null) { //If the user exists
           String token = UUID.randomUUID().toString(); //Generate a token
           user.setVerificationToken(token); //Set the token
           user.setTokenExpiry(LocalDateTime.now().plusHours(1)); //Set the token expiry to 1 hour from now
           em.merge(user); //Merge the changes with the user
           em.flush(); //Execute changes
           try {
               emailService.sendPasswordResetEmail(email, token); //Send the password reset email with the generated token
           } catch (Exception e) {
               throw new Exception("Failed to send password reset email: " + e.getMessage()); //If failed to send email throw an exception
           }
       }
   }

   /**
    * Resets a user's password using a token and a new password.
    *
    * @param token the reset token
    * @param newPassword the new password
    * @return true if the password is successfully reset, false otherwise
    * @throws Exception if the password is not valid or the token is expired
    */
   @Transactional
   public boolean resetPassword(String token, String newPassword) throws Exception {
       try {
           validatePassword(newPassword); //Validate the new password
           User user = findUserByResetToken(token); //Find the user by the reset token

           if (user != null) {
               Timestamp currentTime = new Timestamp(System.currentTimeMillis()); //Get the current time
               if (user.getTokenExpiry() != null && user.getTokenExpiry().isAfter(currentTime.toLocalDateTime())) { //If the token expiry is valid and is after the current time
                   user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt())); //Set the new password
                   user.setVerificationToken(null); //Remove the verification token
                   user.setTokenExpiry(null); //Remove the token expiry
                   em.merge(user); //Merge the changes with the user
                   em.flush(); //Execute changes
                   return true; //Password reset successful
               }
               else throw new Exception("Token expired"); //Throw exception if the token is expired   
           }
       } catch (Exception e) {
           throw e;
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
           User user = findUserByEmail(email); //Find the user by the input email
           if (user == null) { //If the user is not found
               throw new Exception("No account found with this email address."); //Throw a user not found exception
           }
           
           if (user.getIsVerified()) { //If the user is verified
               throw new Exception("This account is already verified."); //Throw an account already verified exception
           }

           String token = UUID.randomUUID().toString(); //Generate a new verification token
           user.setVerificationToken(token); //Set the verification token
           user.setTokenExpiry(LocalDateTime.now().plusHours(24)); //Set the token expiry to 1 day from now
           em.merge(user); //Merge the changes with the user
           em.flush(); //Execute changes
           emailService.sendVerificationEmail(user.getEmail(), token); //Send the verification email with the generated token
       } catch (Exception e) {
           throw e;
       }
   }

   /**
    * Updates a user's information (email, first name, last name, iban).
    *
    * @param user the user to update
    * @throws Exception if the user is not found or update fails
    */
   @Transactional
   public void updateUser(User user) throws Exception {
       try {
           User existingUser = findUserByEmail(user.getEmail()); //Find the user by the input email
           if (existingUser == null) throw new Exception("User not found"); //Throw an exception if the user is not found

           em.merge(user); //Merge the changes with the user
           em.flush(); //Execute changes
       } catch (Exception e) {
           throw new Exception("Failed to update user: " + e.getMessage()); //Throw an exception if the user is not found
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
           validatePassword(newPassword); //Validate the new password
           user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt())); //Set the new password
           em.merge(user); //Merge the changes with the user
           em.flush(); //Force any pending database operations to be executed immediately
       } catch (ValidationException e) {
           throw new Exception("Failed to update password: " + e.getMessage()); //If failed to update password throw an exception
       }
   }

   /**
    * Verifies a user's password.
    *
    * @param email the user's email
    * @param password the password to verify
    * @return true if the password is correct, false otherwise
    * @throws Exception if the user is not found
    */
   public boolean verifyPassword(String email, String password) throws Exception {
       try {
           User user = findUserByEmail(email); //Find the user by the input email
           return user != null && BCrypt.checkpw(password, user.getPassword()); //Return true if the password is correct, false otherwise
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
       GoogleAuthenticator gAuthNew = new GoogleAuthenticator(); //Create a new GoogleAuthenticator
       GoogleAuthenticatorKey key = gAuthNew.createCredentials(); //Create a new GoogleAuthenticatorKey
       String secret = key.getKey(); //Get the secret
       return secret; //Return the secret
   }

   /**
    * Finds a user by their reset token.
    *
    * @param token the reset token
    * @return the User object if found, null otherwise
    */
   public User findUserByResetToken(String token) {
       try {
           return em.createQuery("SELECT u FROM User u WHERE u.verificationToken = :token", User.class) //Select the user by the reset token
                    .setParameter("token", token)
                    .getSingleResult(); //Return the user
       } catch (Exception e) {
           return null; //If no user is found return null
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
       emailService.sendPasswordResetEmail(email, token); //Send the password reset email with the generated token
   }

   /**
    * Generates a unique variable symbol.
    *
    * @return a unique variable symbol
    */
   private String generateVariableSymbol() {
       String variableSymbol; //Variable symbol to return
       boolean isUnique; //Boolean to check if the variable symbol is unique    

       do {
           long randomNumber = (long) (Math.random() * 1_000_000_000L); //Generate a random number
           variableSymbol = String.format("%010d", randomNumber); //Format the number to be 10 characters long

           isUnique = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.variableSymbol = :variableSymbol", Long.class) //Check if the variable symbol is unique
                        .setParameter("variableSymbol", variableSymbol)
                        .getSingleResult() == 0; //Check if the variable symbol is unique
       } while (!isUnique); //If the variable symbol is not unique, generate a new one

       return variableSymbol; //Return the variable symbol
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
           return em.createQuery("SELECT u.balance FROM User u WHERE u.id = :userId", Double.class) //Select the balance from the db and return the double
                    .setParameter("userId", userId)
                    .getSingleResult(); //Return the balance
       } catch (Exception e) {
           return 0.0; //If no balance is found return 0.0
       }
   }

   /**
    * Deletes a user account and all associated data
    * @param user The user to delete
    * @param password The user's password for confirmation
    * @throws Exception if deletion fails or password is incorrect
    */
   @Transactional
   public void deleteUserAccount(User user, String password) throws Exception {
       if (!BCrypt.checkpw(password, user.getPassword())) { //If the password is incorrect throw an exception
           throw new SecurityException("Incorrect password");
       }
       if(user.getBalance() >= 0.01) { //If the user has a balance throw an exception
           throw new Exception("Your balance must be 0 to delete the account");
       }
       User deletedAccount = findUserByEmail("scacciadavide@gmail.com"); //Get the account with "Deleted Account" as name

       try {
           em.createQuery("UPDATE Transaction t SET t.sender = :deletedAccount WHERE t.sender.id = :userId") //Update transactions to set sender to deleted account where user is sender
             .setParameter("deletedAccount", deletedAccount)
             .setParameter("userId", user.getId())
             .executeUpdate();
           em.createQuery("UPDATE Transaction t SET t.receiver = :deletedAccount WHERE t.receiver.id = :userId") //Update transactions to set receiver to deleted account where user is receiver
             .setParameter("deletedAccount", deletedAccount)
             .setParameter("userId", user.getId())
             .executeUpdate();
            /*em.createQuery("DELETE FROM requestMoney WHERE sender.id = :userId OR receiver.id = :userId") //Delete money requests where user is sender or receiver
              .setParameter("userId", user.getId())
              .executeUpdate();*/
        
           User managedUser = em.merge(user); //Merge the changes with the user
           em.remove(managedUser); //Remove the user
           em.flush(); //Execute changes
       } catch (Exception e) {
           throw new Exception("Failed to delete account: " + e.getMessage()); //If failed to delete account throw an exception
       }
   }
}
