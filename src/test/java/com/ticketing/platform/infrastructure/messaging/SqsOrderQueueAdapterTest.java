package com.ticketing.platform.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.port.out.OrderQueuePort.OrderQueueMessage;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@ExtendWith(MockitoExtension.class)
class SqsOrderQueueAdapterTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Test
    void shouldPublishMessagesToSqsQueue() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSqs().setQueueName("orders");
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
            CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("http://queue-url").build())
        );
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(SendMessageResponse.builder().messageId("m1").build())
        );

        SqsOrderQueueAdapter adapter = new SqsOrderQueueAdapter(sqsAsyncClient, properties);
        UUID orderId = UUID.randomUUID();

        adapter.publish(orderId).block();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient, times(1)).sendMessage(requestCaptor.capture());
        assertThat(requestCaptor.getValue().messageBody()).isEqualTo(orderId.toString());
    }

    @Test
    void shouldReceiveAndAcknowledgeMessages() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSqs().setPollIntervalMs(1000);
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
            CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("http://queue-url").build())
        );

        UUID orderId = UUID.randomUUID();
        Message message = Message.builder()
            .body(orderId.toString())
            .receiptHandle("receipt-1")
            .messageId("id-1")
            .build();
        when(sqsAsyncClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(message).build())
        );
        when(sqsAsyncClient.deleteMessage(any(DeleteMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(DeleteMessageResponse.builder().build())
        );

        SqsOrderQueueAdapter adapter = new SqsOrderQueueAdapter(sqsAsyncClient, properties);

        OrderQueueMessage queueMessage = adapter.receive()
            .next()
            .block(Duration.ofSeconds(2));

        assertThat(queueMessage).isNotNull();
        assertThat(queueMessage.orderId()).isEqualTo(orderId);

        queueMessage.acknowledge().block();
        verify(sqsAsyncClient, times(1)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldDeleteInvalidMessagesWithoutEmittingOrder() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSqs().setPollIntervalMs(1000);
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
            CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("http://queue-url").build())
        );
        Message invalid = Message.builder()
            .body("not-a-uuid")
            .receiptHandle("receipt-invalid")
            .messageId("invalid-id")
            .build();
        when(sqsAsyncClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(invalid).build())
        );
        when(sqsAsyncClient.deleteMessage(any(DeleteMessageRequest.class))).thenReturn(
            CompletableFuture.completedFuture(DeleteMessageResponse.builder().build())
        );

        SqsOrderQueueAdapter adapter = new SqsOrderQueueAdapter(sqsAsyncClient, properties);

        StepVerifier.create(adapter.receive().take(Duration.ofMillis(200)))
            .expectComplete()
            .verify(Duration.ofSeconds(2));

        verify(sqsAsyncClient, times(1)).deleteMessage(any(DeleteMessageRequest.class));
    }
}
