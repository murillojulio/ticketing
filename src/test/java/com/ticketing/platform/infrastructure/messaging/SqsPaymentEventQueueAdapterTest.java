package com.ticketing.platform.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.platform.application.model.PaymentEvent;
import com.ticketing.platform.application.model.PaymentStatus;
import com.ticketing.platform.application.port.out.PaymentEventQueuePort.PaymentEventMessage;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@ExtendWith(MockitoExtension.class)
class SqsPaymentEventQueueAdapterTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldPublishPaymentEventsToSqsQueue() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSqs().setPaymentQueueName("payments");
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
            CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("http://queue-url").build())
        );
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(SendMessageResponse.builder().messageId("m1").build())
        );

        SqsPaymentEventQueueAdapter adapter = new SqsPaymentEventQueueAdapter(sqsAsyncClient, properties, objectMapper);
        PaymentEvent event = new PaymentEvent(
            UUID.randomUUID(),
            "payment-1",
            PaymentStatus.CONFIRMED,
            Instant.parse("2026-02-01T10:00:00Z")
        );

        adapter.publish(event).block();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient, times(1)).sendMessage(requestCaptor.capture());
        assertThat(requestCaptor.getValue().messageBody()).contains("\"paymentId\":\"payment-1\"");
    }

    @Test
    void shouldReceiveAndAcknowledgePaymentEvents() throws Exception {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSqs().setPollIntervalMs(1000);
        properties.getSqs().setPaymentQueueName("payments");
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
            CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("http://queue-url").build())
        );

        PaymentEvent event = new PaymentEvent(
            UUID.randomUUID(),
            "payment-2",
            PaymentStatus.FAILED,
            Instant.parse("2026-02-01T10:00:00Z")
        );
        Message message = Message.builder()
            .body(objectMapper.writeValueAsString(event))
            .receiptHandle("receipt-1")
            .messageId("id-1")
            .build();
        when(sqsAsyncClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(message).build())
        );
        when(sqsAsyncClient.deleteMessage(any(DeleteMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(DeleteMessageResponse.builder().build())
        );

        SqsPaymentEventQueueAdapter adapter = new SqsPaymentEventQueueAdapter(sqsAsyncClient, properties, objectMapper);

        PaymentEventMessage queueMessage = adapter.receive()
            .next()
            .block(Duration.ofSeconds(2));

        assertThat(queueMessage).isNotNull();
        assertThat(queueMessage.paymentEvent().paymentId()).isEqualTo("payment-2");

        queueMessage.acknowledge().block();
        verify(sqsAsyncClient, times(1)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldDeleteInvalidMessagesWithoutEmittingEvent() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSqs().setPollIntervalMs(1000);
        properties.getSqs().setPaymentQueueName("payments");
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
            CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("http://queue-url").build())
        );
        Message invalid = Message.builder()
            .body("{invalid-json}")
            .receiptHandle("receipt-invalid")
            .messageId("invalid-id")
            .build();
        when(sqsAsyncClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(invalid).build())
        );
        when(sqsAsyncClient.deleteMessage(any(DeleteMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(DeleteMessageResponse.builder().build())
        );

        SqsPaymentEventQueueAdapter adapter = new SqsPaymentEventQueueAdapter(sqsAsyncClient, properties, objectMapper);

        StepVerifier.create(adapter.receive().take(Duration.ofMillis(200)))
            .expectComplete()
            .verify(Duration.ofSeconds(2));

        verify(sqsAsyncClient, times(1)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldCreatePaymentQueueWhenMissing() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSqs().setPaymentQueueName("payments");
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
            CompletableFuture.failedFuture(QueueDoesNotExistException.builder().message("missing queue").build()),
            CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("http://queue-url").build())
        );
        when(sqsAsyncClient.createQueue(any(CreateQueueRequest.class))).thenReturn(
            CompletableFuture.completedFuture(CreateQueueResponse.builder().queueUrl("http://queue-url").build())
        );
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(SendMessageResponse.builder().messageId("m1").build())
        );

        SqsPaymentEventQueueAdapter adapter = new SqsPaymentEventQueueAdapter(sqsAsyncClient, properties, objectMapper);
        adapter.publish(new PaymentEvent(
            UUID.randomUUID(),
            "payment-3",
            PaymentStatus.CONFIRMED,
            Instant.parse("2026-02-01T10:00:00Z")
        )).block();

        verify(sqsAsyncClient, times(1)).createQueue(any(CreateQueueRequest.class));
        verify(sqsAsyncClient, times(2)).getQueueUrl(any(GetQueueUrlRequest.class));
    }
}
