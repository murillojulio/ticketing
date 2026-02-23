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

        assertThat(properties.getReservationHold()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getQueueRetryAttempts()).isEqualTo(5);
        assertThat(properties.getConsumerConcurrency()).isEqualTo(8);
        assertThat(properties.getExpirationCheckIntervalMs()).isEqualTo(45_000);
    }
}
