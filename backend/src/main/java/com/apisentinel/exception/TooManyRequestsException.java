package com.apisentinel.exception;

import java.time.Duration;

public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    // Rounded up to whole seconds, so a client that waits exactly this long is allowed again.
    public TooManyRequestsException(Duration retryAfter) {
        this(Math.max(1, (retryAfter.toMillis() + 999) / 1000));
    }

    private TooManyRequestsException(long retryAfterSeconds) {
        super("Too many requests. Try again in " + describe(retryAfterSeconds) + ".");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String describe(long seconds) {
        if (seconds < 60) {
            return seconds == 1 ? "1 second" : seconds + " seconds";
        }
        long minutes = (seconds + 59) / 60;
        return minutes == 1 ? "1 minute" : minutes + " minutes";
    }
}
