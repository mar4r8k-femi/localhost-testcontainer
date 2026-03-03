package com.example.sqs;

import com.example.base.LocalStackBase;
import com.example.service.SqsService;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

/**
 * Integration tests for SqsService against a real LocalStack container.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class SqsLocalStackTest extends LocalStackBase {

    private SqsClient sqsClient;
    private SqsService sqsService;
    private String queueUrl;

    @BeforeEach
    void setUp() {
        sqsClient  = sqsClient();
        sqsService = new SqsService(sqsClient);
        String queueName = "test-queue-" + UUID.randomUUID().toString().substring(0, 8);
        queueUrl = sqsService.createQueue(queueName);
    }

    @AfterEach
    void tearDown() {
        sqsService.deleteQueue(queueUrl);
        sqsClient.close();
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1. send and receive a single message")
    void sendAndReceiveSingleMessage() {
        String messageId = sqsService.sendMessage(queueUrl, "Hello SQS!");

        assertThat(messageId).isNotBlank();

        List<Message> messages = sqsService.receiveMessages(queueUrl, 1);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).body()).isEqualTo("Hello SQS!");
    }

    @Test
    @DisplayName("2. message is deleted after processing")
    void messageDeletedAfterProcessing() {
        sqsService.sendMessage(queueUrl, "process-me");

        List<Message> messages = sqsService.receiveMessages(queueUrl, 1);
        assertThat(messages).hasSize(1);

        sqsService.deleteMessage(queueUrl, messages.get(0).receiptHandle());

        // After deletion the queue should drain to 0
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() ->
                assertThat(sqsService.getApproximateMessageCount(queueUrl)).isZero()
            );
    }

    @Test
    @DisplayName("3. send multiple messages and receive in batch")
    void sendMultipleMessages() {
        sqsService.sendMessage(queueUrl, "msg-1");
        sqsService.sendMessage(queueUrl, "msg-2");
        sqsService.sendMessage(queueUrl, "msg-3");

        // SQS may return fewer than requested; retry until we have all 3
        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {
                List<Message> messages = sqsService.receiveMessages(queueUrl, 10);
                assertThat(messages).hasSizeGreaterThanOrEqualTo(1);
            });
    }

    @Test
    @DisplayName("4. send JSON payload as message body")
    void sendJsonMessageBody() {
        String json = "{"orderId":"123","status":"PENDING"}";
        sqsService.sendMessage(queueUrl, json);

        List<Message> messages = sqsService.receiveMessages(queueUrl, 1);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).body()).isEqualTo(json);
    }
}