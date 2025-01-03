package beans.services;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RateLimiterService {
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;
    
    private final Map<String, AtomicInteger> attemptCount = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockoutTime = new ConcurrentHashMap<>();

    public boolean isAllowed(String email) {
        cleanupExpiredEntries();
        
        // Check if user is locked out
        LocalDateTime lockoutEndTime = lockoutTime.get(email);
        if (lockoutEndTime != null && LocalDateTime.now().isBefore(lockoutEndTime)) {
            return false;
        }

        // Reset if lockout has expired
        if (lockoutEndTime != null && LocalDateTime.now().isAfter(lockoutEndTime)) {
            attemptCount.remove(email);
            lockoutTime.remove(email);
        }

        return attemptCount.getOrDefault(email, new AtomicInteger(0))
                         .get() < MAX_ATTEMPTS;
    }

    public void recordFailedAttempt(String email) {
        AtomicInteger attempts = attemptCount.computeIfAbsent(email, k -> new AtomicInteger(0));
        if (attempts.incrementAndGet() >= MAX_ATTEMPTS) {
            lockoutTime.put(email, LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
        }
    }

    public void resetAttempts(String email) {
        attemptCount.remove(email);
        lockoutTime.remove(email);
    }

    private void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        lockoutTime.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }

    public int getRemainingAttempts(String email) {
        return MAX_ATTEMPTS - attemptCount.getOrDefault(email, new AtomicInteger(0)).get();
    }

    public LocalDateTime getLockoutEndTime(String email) {
        return lockoutTime.get(email);
    }
}
