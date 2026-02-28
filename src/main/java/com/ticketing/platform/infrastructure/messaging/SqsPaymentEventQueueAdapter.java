package com.ticketing.platform.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.platform.application.model.PaymentEvent;
import com.ticketing.platform.application.port.out.PaymentEventQueuePort;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.time.Duration;
import java.util.Map;
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
public class SqsPaymentEventQueueAdapter implements PaymentEventQueuePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsPaymentEventQueueAdapter.class);

    private final SqsAsyncClient sqsAsyncClient;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;

    public SqsPaymentEventQueueAdapter(
        SqsAsyncClient sqsAsyncClient,
        ApplicationProperties applicationProperties,
        ObjectMapper objectMapper
    ) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> publish(PaymentEvent paymentEvent) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(paymentEvent))
            .flatMap(payload -> queueUrl().flatMap(queueUrl -> Mono.fromFuture(sqsAsyncClient.sendMessage(
                    SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(payload)
                        .build()
                ))
                .then()))
            .onErrorMap(JsonProcessingException.class, error ->
                new IllegalArgumentException("Invalid payment event payload", error)
            );
    }

    @Override
    public Flux<PaymentEventMessage> receive() {
        return Flux.interval(
                Duration.ZERO,
                Duration.ofMillis(applicationProperties.getSqs().getPollIntervalMs())
            )
            .onBackpressureDrop()
            .concatMap(ignored -> pollQueue());
    }

    private Flux<PaymentEventMessage> pollQueue() {
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

    private Mono<PaymentEventMessage> toQueueMessage(String queueUrl, Message message) {
        try {
            PaymentEvent paymentEvent = objectMapper.readValue(message.body(), PaymentEvent.class);
            Mono<Void> acknowledge = Mono.fromFuture(sqsAsyncClient.deleteMessage(
                    DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build()
                ))
                .then();
            return Mono.just(new PaymentEventMessage(paymentEvent, acknowledge));
        } catch (Exception error) {
            LOGGER.error("Invalid payment event message '{}'. Message will be deleted.", message.body(), error);
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
                    .queueName(applicationProperties.getSqs().getPaymentQueueName())
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
                    .queueName(sqsProperties.getPaymentQueueName())
                    .attributes(Map.of(
                        QueueAttributeName.VISIBILITY_TIMEOUT, String.valueOf(sqsProperties.getVisibilityTimeoutSeconds()),
                        QueueAttributeName.MESSAGE_RETENTION_PERIOD, "1209600"
                    ))
                    .build()
            ))
            .doOnNext(response -> LOGGER.info(
                "SQS payment queue '{}' is ready at {}",
                sqsProperties.getPaymentQueueName(),
                response.queueUrl()
            ))
            .then();
    }

    private boolean isQueueMissingError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof QueueDoesNotExistException) {
                LOGGER.warn(
                    "SQS payment queue '{}' not found. Creating it automatically.",
                    applicationProperties.getSqs().getPaymentQueueName()
                );
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
