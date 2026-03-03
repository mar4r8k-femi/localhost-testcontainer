package com.example.service;

import com.example.model.OrderItem;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Optional;

public class DynamoDbService {

    public static final String TABLE_NAME = "order-items";

    private final DynamoDbEnhancedClient enhanced;
    private final DynamoDbTable<OrderItem> table;

    public DynamoDbService(DynamoDbClient dynamoDb) {
        this.enhanced = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDb)
            .build();
        this.table = enhanced.table(TABLE_NAME, TableSchema.fromBean(OrderItem.class));
    }

    public void createTableIfNotExists() {
        try {
            table.createTable(CreateTableEnhancedRequest.builder()
                .provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build())
                .build());
        } catch (ResourceInUseException ignored) {}
    }

    public void save(OrderItem item) {
        table.putItem(item);
    }

    public Optional<OrderItem> findByKey(String orderId, String itemId) {
        Key key = Key.builder()
            .partitionValue(orderId)
            .sortValue(itemId)
            .build();
        return Optional.ofNullable(table.getItem(key));
    }

    public List<OrderItem> findByOrderId(String orderId) {
        QueryConditional qc = QueryConditional
            .keyEqualTo(Key.builder().partitionValue(orderId).build());
        return table.query(qc).items().stream().toList();
    }

    public void delete(String orderId, String itemId) {
        table.deleteItem(Key.builder()
            .partitionValue(orderId).sortValue(itemId).build());
    }

    public void updateQuantity(String orderId, String itemId, int newQty) {
        findByKey(orderId, itemId).ifPresent(item -> {
            item.setQuantity(newQty);
            table.updateItem(item);
        });
    }
}