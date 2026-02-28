package com.ticketing.platform.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ApplicationPropertiesTest {

    @Test
    void shouldExposeMutableConfigurationValues() {
        ApplicationProperties properties = new ApplicationProperties();

        properties.setReservationHold(Duration.ofMinutes(15));
        properties.setQueueRetryAttempts(5);
        properties.setConsumerConcurrency(8);
        properties.setExpirationCheckIntervalMs(45_000);
        ApplicationProperties.Adapters adapters = new ApplicationProperties.Adapters();
        adapters.setPersistence("mongo");
        adapters.setQueue("sqs");
        properties.setAdapters(adapters);
        ApplicationProperties.Sqs sqs = new ApplicationProperties.Sqs();
        sqs.setEndpoint("http://localhost:4566");
        sqs.setRegion("us-east-1");
        sqs.setAccessKey("test");
        sqs.setSecretKey("test");
        sqs.setQueueName("ticketing-order-processing");
        sqs.setPaymentQueueName("ticketing-payment-events");
        sqs.setMaxMessages(5);
        sqs.setWaitTimeSeconds(8);
        sqs.setVisibilityTimeoutSeconds(60);
        sqs.setPollIntervalMs(250);
        properties.setSqs(sqs);
        ApplicationProperties.Security security = new ApplicationProperties.Security();
        security.setEnabled(true);
        ApplicationProperties.Jwt jwt = new ApplicationProperties.Jwt();
        jwt.setSecret("this-is-a-long-test-secret-with-at-least-thirty-two-bytes");
        jwt.setExpirationMinutes(90);
        security.setJwt(jwt);
        properties.setSecurity(security);

        assertThat(properties.getReservationHold()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getQueueRetryAttempts()).isEqualTo(5);
        assertThat(properties.getConsumerConcurrency()).isEqualTo(8);
        assertThat(properties.getExpirationCheckIntervalMs()).isEqualTo(45_000);
        assertThat(properties.getAdapters().getPersistence()).isEqualTo("mongo");
        assertThat(properties.getAdapters().getQueue()).isEqualTo("sqs");
        assertThat(properties.getSqs().getEndpoint()).isEqualTo("http://localhost:4566");
        assertThat(properties.getSqs().getRegion()).isEqualTo("us-east-1");
        assertThat(properties.getSqs().getAccessKey()).isEqualTo("test");
        assertThat(properties.getSqs().getSecretKey()).isEqualTo("test");
        assertThat(properties.getSqs().getQueueName()).isEqualTo("ticketing-order-processing");
        assertThat(properties.getSqs().getPaymentQueueName()).isEqualTo("ticketing-payment-events");
        assertThat(properties.getSqs().getMaxMessages()).isEqualTo(5);
        assertThat(properties.getSqs().getWaitTimeSeconds()).isEqualTo(8);
        assertThat(properties.getSqs().getVisibilityTimeoutSeconds()).isEqualTo(60);
        assertThat(properties.getSqs().getPollIntervalMs()).isEqualTo(250);
        assertThat(properties.getSecurity().isEnabled()).isTrue();
        assertThat(properties.getSecurity().getJwt().getSecret()).isEqualTo(
            "this-is-a-long-test-secret-with-at-least-thirty-two-bytes"
        );
        assertThat(properties.getSecurity().getJwt().getExpirationMinutes()).isEqualTo(90);
    }
}
