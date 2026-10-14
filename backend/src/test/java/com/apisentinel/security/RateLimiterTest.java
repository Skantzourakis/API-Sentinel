package com.apisentinel.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    // A clock the test moves forward by hand, so windows can expire without waiting.
    private static final class TestClock extends Clock {

        private Instant now = Instant.parse("2026-01-01T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final TestClock clock = new TestClock();
    private final RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), clock);

    @Test
    void allowsUpToTheLimitAndThenReportsHowLongToWait() {
        assertThat(limiter.acquire("1.2.3.4")).isEmpty();
        assertThat(limiter.acquire("1.2.3.4")).isEmpty();
        assertThat(limiter.acquire("1.2.3.4")).isEmpty();

        assertThat(limiter.acquire("1.2.3.4")).contains(Duration.ofMinutes(1));
        clock.advance(Duration.ofSeconds(20));
        assertThat(limiter.acquire("1.2.3.4")).contains(Duration.ofSeconds(40));
    }

    @Test
    void startsCountingAgainInTheNextWindow() {
        for (int i = 0; i < 4; i++) {
            limiter.acquire("1.2.3.4");
        }

        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.acquire("1.2.3.4")).isEmpty();
    }

    @Test
    void countsEveryKeySeparately() {
        for (int i = 0; i < 4; i++) {
            limiter.acquire("1.2.3.4");
        }

        assertThat(limiter.acquire("5.6.7.8")).isEmpty();
    }

    @Test
    void resetClearsTheCount() {
        for (int i = 0; i < 4; i++) {
            limiter.acquire("user@example.com");
        }

        limiter.reset("user@example.com");
        assertThat(limiter.acquire("user@example.com")).isEmpty();
    }
}
