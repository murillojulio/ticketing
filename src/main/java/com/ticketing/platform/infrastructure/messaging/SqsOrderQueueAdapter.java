package com.ticketing.platform.infrastructure.messaging;

import com.ticketing.platform.application.port.out.OrderQueuePort;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@ConditionalOnProperty(prefix = "ticketing.adapters", name = "queue", havingValue = "sqs", matchIfMissing = true)
public class SqsOrderQueueAdapter implements OrderQueuePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsOrderQueueAdapter.class);

    private final SqsAsyncClient sqsAsyncClient;
    private final ApplicationProperties applicationProperties;

    public SqsOrderQueueAdapter(
        SqsAsyncClient sqsAsyncClient,
        ApplicationProperties applicationProperties
    ) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public Mono<Void> publish(UUID orderId) {
        return queueUrl().flatMap(queueUrl -> Mono.fromFuture(sqsAsyncClient.sendMessage(
                SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(orderId.toString())
                    .build()
            ))
            .then());
    }

    @Override
    public Flux<OrderQueueMessage> receive() {
        return Flux.interval(
                Duration.ZERO,
                Duration.ofMillis(applicationProperties.getSqs().getPollIntervalMs())
            )
            .onBackpressureDrop()
            .concatMap(ignored -> pollQueue());
    }

    private Flux<OrderQueueMessage> pollQueue() {
        ApplicationProperties.Sqs sqsProperties = applicationProperties.getSqs();
        return queueUrl().flatMapMany(queueUrl -> Mono.fromFuture(sqsAsyncClient.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(sqsProperties.getMaxMessages())
                    .waitTimeSeconds(sqsProperties.getWaitTimeSeconds())
                    .visibilityTimeout(sqsProperties.getVisibilityTimeoutSeconds())
                    .build()
            ))
            .flatMapMany(response -> Flux.fromIterable(response.messages()))
            .flatMap(message -> toQueueMessage(queueUrl, message)));
    }

    private Mono<OrderQueueMessage> toQueueMessage(String queueUrl, Message message) {
        try {
            UUID orderId = UUID.fromString(message.body());
            Mono<Void> acknowledge = Mono.fromFuture(sqsAsyncClient.deleteMessage(
                    DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build()
                ))
                .then();
            return Mono.just(new OrderQueueMessage(orderId, acknowledge));
        } catch (IllegalArgumentException error) {
            LOGGER.error("Invalid order message payload '{}'. Message will be deleted.", message.body(), error);
            return Mono.fromFuture(sqsAsyncClient.deleteMessage(
                    DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build()
                ))
                .then(Mono.empty());
        }
    }

    private Mono<String> resolveQueueUrl() {
        return Mono.fromFuture(sqsAsyncClient.getQueueUrl(
                GetQueueUrlRequest.builder()
                    .queueName(applicationProperties.getSqs().getQueueName())
                    .build()
            ))
            .map(response -> response.queueUrl());
    }

    private Mono<String> resolveOrCreateQueueUrl() {
        return resolveQueueUrl()
            .onErrorResume(this::isQueueMissingError, error -> createQueueIfMissing().then(resolveQueueUrl()));
    }

    private Mono<Void> createQueueIfMissing() {
        ApplicationProperties.Sqs sqsProperties = applicationProperties.getSqs();
        return Mono.fromFuture(sqsAsyncClient.createQueue(
                CreateQueueRequest.builder()
                    .queueName(sqsProperties.getQueueName())
                    .attributes(Map.of(
                        QueueAttributeName.VISIBILITY_TIMEOUT, String.valueOf(sqsProperties.getVisibilityTimeoutSeconds()),
                        QueueAttributeName.MESSAGE_RETENTION_PERIOD, "1209600"
                    ))
                    .build()
            ))
            .doOnNext(response -> LOGGER.info("SQS queue '{}' is ready at {}", sqsProperties.getQueueName(), response.queueUrl()))
            .then();
    }

    private boolean isQueueMissingError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof QueueDoesNotExistException) {
                LOGGER.warn("SQS queue '{}' not found. Creating it automatically.", applicationProperties.getSqs().getQueueName());
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Mono<String> queueUrl() {
        return resolveOrCreateQueueUrl()
            .retryWhen(Retry.backoff(20, Duration.ofMillis(250)));
    }
}
