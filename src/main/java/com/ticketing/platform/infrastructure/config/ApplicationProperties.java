package com.ticketing.platform.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticketing")
public class ApplicationProperties {

    private Duration reservationHold = Duration.ofMinutes(10);
    private int queueRetryAttempts = 3;
    private int consumerConcurrency = 4;
    private long expirationCheckIntervalMs = 30_000;
    private Adapters adapters = new Adapters();
    private Sqs sqs = new Sqs();

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

    public Adapters getAdapters() {
        return adapters;
    }

    public void setAdapters(Adapters adapters) {
        this.adapters = adapters;
    }

    public Sqs getSqs() {
        return sqs;
    }

    public void setSqs(Sqs sqs) {
        this.sqs = sqs;
    }

    public static class Adapters {

        private String persistence = "mongo";
        private String queue = "sqs";

        public String getPersistence() {
            return persistence;
        }

        public void setPersistence(String persistence) {
            this.persistence = persistence;
        }

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }
    }

    public static class Sqs {

        private String endpoint = "http://localhost:4566";
        private String region = "us-east-1";
        private String accessKey = "test";
        private String secretKey = "test";
        private String queueName = "ticketing-order-processing";
        private String paymentQueueName = "ticketing-payment-events";
        private int maxMessages = 10;
        private int waitTimeSeconds = 10;
        private int visibilityTimeoutSeconds = 45;
        private long pollIntervalMs = 200;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }

        public String getPaymentQueueName() {
            return paymentQueueName;
        }

        public void setPaymentQueueName(String paymentQueueName) {
            this.paymentQueueName = paymentQueueName;
        }

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }

        public int getWaitTimeSeconds() {
            return waitTimeSeconds;
        }

        public void setWaitTimeSeconds(int waitTimeSeconds) {
            this.waitTimeSeconds = waitTimeSeconds;
        }

        public int getVisibilityTimeoutSeconds() {
            return visibilityTimeoutSeconds;
        }

        public void setVisibilityTimeoutSeconds(int visibilityTimeoutSeconds) {
            this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
        }

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }
    }
}
