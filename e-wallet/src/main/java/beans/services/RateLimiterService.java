package beans.services;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for limiting the rate of login attempts made by a user for a specific email.
 * Tracks the number of attempts made by a user to login and locks them out
 * for 5 minutes if they exceed the maximum allowed attempts (5).
 * 
 * @author Davide Scaccia - xscaccd00
 */

@ApplicationScoped
public class RateLimiterService {
    private static final int MAX_ATTEMPTS = 5; //Maximum number of attempts
    private static final int LOCK_DURATION_MINUTES = 5; //Lockout duration (minutes)
    
    private final Map<String, AtomicInteger> attemptCount = new ConcurrentHashMap<>(); //Map of each email to the number of attempts
    private final Map<String, LocalDateTime> lockoutTime = new ConcurrentHashMap<>(); //Map of each email to the lockout time

    /**
     * Checks if the user is allowed to login based on their email.
     * 
     * @param email the email of the user
     * @return true if the user is allowed to login, false if they are locked out
     */
    public boolean isAllowed(String email) {
        cleanupExpiredEntries(); //Clean up expired entries
        
        LocalDateTime lockoutEndTime = lockoutTime.get(email); //Get the lockout end time
        if (lockoutEndTime != null && LocalDateTime.now().isBefore(lockoutEndTime)) { //Return false if the user is locked out
            return false;
        }

        if (lockoutEndTime != null && LocalDateTime.now().isAfter(lockoutEndTime)) { //Remove the attempt count and lockoutTime if the user is not locked out anymore
            attemptCount.remove(email);
            lockoutTime.remove(email);
        }

        return attemptCount.getOrDefault(email, new AtomicInteger(0)).get() < MAX_ATTEMPTS; //Return true if the user is allowed to login
    }

    /**
     * Records a failed attempt for the user identified by their email.
     * If the maximum number of attempts is reached, the user is locked out for 30 minutes.
     * 
     * @param email the email of the user
     */
    public void recordFailedAttempt(String email) {
        AtomicInteger attempts = attemptCount.computeIfAbsent(email, k -> new AtomicInteger(0)); //If the email is not present in the map, create a new AtomicInteger with a value of 0
        if (attempts.incrementAndGet() >= MAX_ATTEMPTS) { //If the number of attempts is greater than or equal to the maximum allowed attempts, lock the user out for 5 minutes
            lockoutTime.put(email, LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
        }
    }

    /** 
     * Resets the attempt count and lockout time for the user identified by their email.
     * 
     * @param email the email of the user
     */
    public void resetAttempts(String email) {
        attemptCount.remove(email);
        lockoutTime.remove(email);
    }

    /**
     * Cleans up expired lockout entries from the map.
     */
    private void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        lockoutTime.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }

    /**
     * Gets the number of remaining attempts for the user identified by their email.
     * 
     * @param email the email of the user
     * @return the number of remaining attempts
     */
    public int getRemainingAttempts(String email) {
        return MAX_ATTEMPTS - attemptCount.getOrDefault(email, new AtomicInteger(0)).get();
    }

    /**
     * Gets the lockout end time for the user identified by their email.
     * 
     * @param email the email of the user
     * @return the lockout end time, or null if the user is not locked out
     */
    public LocalDateTime getLockoutEndTime(String email) {
        return lockoutTime.get(email);
    }
}
