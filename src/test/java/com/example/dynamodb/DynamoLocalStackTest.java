package com.example.dynamodb;

import com.example.base.LocalStackBase;
import com.example.model.OrderItem;
import com.example.service.DynamoDbService;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DynamoDbService against a real LocalStack container.
 *
 * The {@code @Container} annotation on a static field gives this test class
 * its own DynamoDB container scoped to the class lifetime: Testcontainers
 * starts it before the first test and discards it after the last. No shared
 * state leaks between classes, and no manual cleanup of table rows is needed —
 * the container is simply thrown away.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.DisplayName.class)
class DynamoLocalStackTest extends LocalStackBase {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(IMAGE)
        .withServices(Service.DYNAMODB);

    private DynamoDbClient dynamoDbClient;
    private DynamoDbService dynamoDbService;

    @BeforeEach
    void setUp() {
        dynamoDbClient  = dynamoDbClient(localstack);
        dynamoDbService = new DynamoDbService(dynamoDbClient);
        dynamoDbService.createTableIfNotExists();
    }

    @AfterEach
    void tearDown() {
        dynamoDbClient.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private OrderItem buildItem(String orderId, String itemId,
                                 String product, int qty, double price) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setItemId(itemId);
        item.setProductName(product);
        item.setQuantity(qty);
        item.setPrice(price);
        return item;
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1. save and retrieve an item by composite key")
    void saveAndRetrieveItem() {
        OrderItem item = buildItem("order-1", "item-1", "Widget", 3, 9.99);
        dynamoDbService.save(item);

        Optional<OrderItem> result = dynamoDbService.findByKey("order-1", "item-1");

        assertThat(result).isPresent();
        assertThat(result.get().getProductName()).isEqualTo("Widget");
        assertThat(result.get().getQuantity()).isEqualTo(3);
        assertThat(result.get().getPrice()).isEqualTo(9.99);
    }

    @Test
    @DisplayName("2. query all items for an order")
    void queryAllItemsForOrder() {
        String orderId = "order-" + UUID.randomUUID().toString().substring(0, 6);
        dynamoDbService.save(buildItem(orderId, "item-1", "Gadget A", 1, 19.99));
        dynamoDbService.save(buildItem(orderId, "item-2", "Gadget B", 2, 29.99));
        dynamoDbService.save(buildItem(orderId, "item-3", "Gadget C", 5,  4.99));

        List<OrderItem> items = dynamoDbService.findByOrderId(orderId);

        assertThat(items).hasSize(3);
        assertThat(items).extracting(OrderItem::getProductName)
            .containsExactlyInAnyOrder("Gadget A", "Gadget B", "Gadget C");
    }

    @Test
    @DisplayName("3. update item quantity")
    void updateItemQuantity() {
        dynamoDbService.save(buildItem("order-2", "item-1", "Thing", 1, 5.00));

        dynamoDbService.updateQuantity("order-2", "item-1", 10);

        Optional<OrderItem> updated = dynamoDbService.findByKey("order-2", "item-1");
        assertThat(updated).isPresent();
        assertThat(updated.get().getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("4. deleted item is no longer found")
    void deleteItem() {
        dynamoDbService.save(buildItem("order-3", "item-1", "Doomed", 1, 1.00));
        assertThat(dynamoDbService.findByKey("order-3", "item-1")).isPresent();

        dynamoDbService.delete("order-3", "item-1");

        assertThat(dynamoDbService.findByKey("order-3", "item-1")).isEmpty();
    }

    @Test
    @DisplayName("5. missing item returns empty Optional")
    void missingItemReturnsEmpty() {
        Optional<OrderItem> result = dynamoDbService.findByKey("no-such-order", "no-such-item");
        assertThat(result).isEmpty();
    }
}
