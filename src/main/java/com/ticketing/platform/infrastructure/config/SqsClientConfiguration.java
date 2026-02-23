package com.ticketing.platform.infrastructure.config;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
@ConditionalOnProperty(prefix = "ticketing.adapters", name = "queue", havingValue = "sqs", matchIfMissing = true)
public class SqsClientConfiguration {

    @Bean
    public SqsAsyncClient sqsAsyncClient(ApplicationProperties properties) {
        ApplicationProperties.Sqs sqsProperties = properties.getSqs();

        return SqsAsyncClient.builder()
            .endpointOverride(URI.create(sqsProperties.getEndpoint()))
            .region(Region.of(sqsProperties.getRegion()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        sqsProperties.getAccessKey(),
                        sqsProperties.getSecretKey()
                    )
                )
            )
            .build();
    }
}
