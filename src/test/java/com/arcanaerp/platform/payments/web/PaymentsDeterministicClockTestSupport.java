package com.arcanaerp.platform.payments.web;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

final class PaymentsDeterministicClockTestSupport {

    static final Instant BASE_TEST_INSTANT = Instant.parse("2026-03-12T00:00:00Z");

    private PaymentsDeterministicClockTestSupport() {}

    @TestConfiguration
    static class Configuration {

        @Bean
        @Primary
        AdjustableClock paymentsTestClock() {
            return new AdjustableClock(BASE_TEST_INSTANT, ZoneOffset.UTC);
        }
    }

    static final class AdjustableClock extends Clock {

        private final Instant baseInstant;
        private final AtomicReference<Instant> instantRef;
        private final ZoneId zoneId;

        AdjustableClock(Instant baseInstant, ZoneId zoneId) {
            this(baseInstant, zoneId, baseInstant);
        }

        private AdjustableClock(Instant baseInstant, ZoneId zoneId, Instant currentInstant) {
            this.baseInstant = Objects.requireNonNull(baseInstant, "baseInstant is required");
            this.zoneId = Objects.requireNonNull(zoneId, "zoneId is required");
            this.instantRef = new AtomicReference<>(Objects.requireNonNull(currentInstant, "currentInstant is required"));
        }

        void resetToBaseInstant() {
            instantRef.set(baseInstant);
        }

        void setInstant(Instant instant) {
            instantRef.set(Objects.requireNonNull(instant, "instant is required"));
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new AdjustableClock(baseInstant, zone, instant());
        }

        @Override
        public Instant instant() {
            return instantRef.get();
        }
    }
}
