package com.ticketing.platform.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticketing")
public class ApplicationProperties {

    private Duration reservationHold = Duration.ofMinutes(10);
    private int queueRetryAttempts = 3;
    private int consumerConcurrency = 4;
    private long expirationCheckIntervalMs = 30_000;

    public Duration getReservationHold() {
        return reservationHold;
    }

    public void setReservationHold(Duration reservationHold) {
        this.reservationHold = reservationHold;
    }

    public int getQueueRetryAttempts() {
        return queueRetryAttempts;
    }

    public void setQueueRetryAttempts(int queueRetryAttempts) {
        this.queueRetryAttempts = queueRetryAttempts;
    }

    public int getConsumerConcurrency() {
        return consumerConcurrency;
    }

    public void setConsumerConcurrency(int consumerConcurrency) {
        this.consumerConcurrency = consumerConcurrency;
    }

    public long getExpirationCheckIntervalMs() {
        return expirationCheckIntervalMs;
    }

    public void setExpirationCheckIntervalMs(long expirationCheckIntervalMs) {
        this.expirationCheckIntervalMs = expirationCheckIntervalMs;
    }
}
