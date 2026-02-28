package com.ticketing.platform.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

class SqsClientConfigurationTest {

    @Test
    void shouldCreateConfiguredSqsClient() {
        ApplicationProperties properties = new ApplicationProperties();
        ApplicationProperties.Sqs sqs = new ApplicationProperties.Sqs();
        sqs.setEndpoint("http://localhost:4566");
        sqs.setRegion("us-east-1");
        sqs.setAccessKey("test");
        sqs.setSecretKey("test");
        properties.setSqs(sqs);

        SqsClientConfiguration configuration = new SqsClientConfiguration();
        SqsAsyncClient client = configuration.sqsAsyncClient(properties);

        assertThat(client).isNotNull();
        client.close();
    }
}
