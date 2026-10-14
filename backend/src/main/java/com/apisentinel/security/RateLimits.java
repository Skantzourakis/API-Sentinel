package com.apisentinel.security;

import com.apisentinel.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

// The limits that protect login, registration and uploads. Counters live in memory, which is enough
// for one running instance; several instances behind a load balancer would need a shared store such as Redis.
@Component
public class RateLimits {

    private final RateLimiter loginsPerAddress;
    private final RateLimiter loginsPerEmail;
    private final RateLimiter registrationsPerAddress;
    private final RateLimiter uploadsPerUser;

    public RateLimits(
            @Value("${app.rate-limit.logins-per-address-per-minute}") int loginsPerAddress,
            @Value("${app.rate-limit.logins-per-email-per-15-minutes}") int loginsPerEmail,
            @Value("${app.rate-limit.registrations-per-address-per-hour}") int registrationsPerAddress,
            @Value("${app.rate-limit.uploads-per-user-per-minute}") int uploadsPerUser
    ) {
        this.loginsPerAddress = new RateLimiter(loginsPerAddress, Duration.ofMinutes(1));
        this.loginsPerEmail = new RateLimiter(loginsPerEmail, Duration.ofMinutes(15));
        this.registrationsPerAddress = new RateLimiter(registrationsPerAddress, Duration.ofHours(1));
        this.uploadsPerUser = new RateLimiter(uploadsPerUser, Duration.ofMinutes(1));
    }

    // Per address stops fast guessing from one machine; per email stops slow guessing of one account from many.
    // Unknown emails are counted too, so the limit does not reveal which accounts exist.
    public void checkLogin(String clientAddress, String email) {
        acquire(loginsPerAddress, clientAddress);
        acquire(loginsPerEmail, email);
    }

    // A successful login clears the account's counter, so a few typos never lock out its owner.
    public void loginSucceeded(String email) {
        loginsPerEmail.reset(email);
    }

    public void checkRegistration(String clientAddress) {
        acquire(registrationsPerAddress, clientAddress);
    }

    public void checkUpload(Long userId) {
        acquire(uploadsPerUser, String.valueOf(userId));
    }

    private void acquire(RateLimiter limiter, String key) {
        limiter.acquire(key).ifPresent(retryAfter -> {
            throw new TooManyRequestsException(retryAfter);
        });
    }
}
