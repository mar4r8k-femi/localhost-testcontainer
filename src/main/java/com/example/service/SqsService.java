package com.example.service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the AWS SQS SDK.
 * This is the production class exercised by SqsLocalStackTest.
 */
public class SqsService {

    private final SqsClient sqs;

    public SqsService(SqsClient sqs) {
        this.sqs = sqs;
    }

    public String createQueue(String queueName) {
        CreateQueueResponse response = sqs.createQueue(
            CreateQueueRequest.builder().queueName(queueName).build()
        );
        return response.queueUrl();
    }

    public String createFifoQueue(String queueName) {
        String name = queueName.endsWith(".fifo") ? queueName : queueName + ".fifo";
        CreateQueueResponse response = sqs.createQueue(
            CreateQueueRequest.builder()
                .queueName(name)
                .attributesWithStrings(Map.of(
                    "FifoQueue", "true",
                    "ContentBasedDeduplication", "true"
                ))
                .build()
        );
        return response.queueUrl();
    }

    public String sendMessage(String queueUrl, String body) {
        SendMessageResponse response = sqs.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .build()
        );
        return response.messageId();
    }

    public List<Message> receiveMessages(String queueUrl, int maxMessages) {
        return sqs.receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(maxMessages)
                .waitTimeSeconds(5) // long polling
                .build()
        ).messages();
    }

    public void deleteMessage(String queueUrl, String receiptHandle) {
        sqs.deleteMessage(
            DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build()
        );
    }

    public int getApproximateMessageCount(String queueUrl) {
        GetQueueAttributesResponse attrs = sqs.getQueueAttributes(
            GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                .build()
        );
        return Integer.parseInt(
            attrs.attributesAsStrings()
                .getOrDefault("ApproximateNumberOfMessages", "0")
        );
    }

    public void deleteQueue(String queueUrl) {
        sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
    }
}