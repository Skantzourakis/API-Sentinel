package com.apisentinel.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Allows at most `limit` events per key (an address, an email, a user) in each time window.
public class RateLimiter {

    // Expired windows are removed once this many keys are stored, so the map cannot grow without bound.
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Window(Instant end, int count) {
    }

    private final int limit;
    private final Duration period;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int limit, Duration period) {
        this(limit, period, Clock.systemUTC());
    }

    RateLimiter(int limit, Duration period, Clock clock) {
        this.limit = limit;
        this.period = period;
        this.clock = clock;
    }

    // Counts one event. Returns how long to wait when the key is over its limit, or empty when it is allowed.
    public Optional<Duration> acquire(String key) {
        Instant now = clock.instant();
        if (windows.size() >= CLEANUP_THRESHOLD) {
            windows.values().removeIf(window -> !window.end().isAfter(now));
        }
        // compute() updates one key atomically, so parallel requests cannot all slip in under the limit.
        Window window = windows.compute(key, (k, current) -> current == null || !current.end().isAfter(now)
                ? new Window(now.plus(period), 1)
                : new Window(current.end(), current.count() + 1));
        return window.count() <= limit ? Optional.empty() : Optional.of(Duration.between(now, window.end()));
    }

    public void reset(String key) {
        windows.remove(key);
    }
}
