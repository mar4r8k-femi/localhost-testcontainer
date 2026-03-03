package com.example.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class OrderItem {

    private String orderId;
    private String itemId;
    private String productName;
    private int quantity;
    private double price;

    @DynamoDbPartitionKey
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @DynamoDbSortKey
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getProductName() { return productName; }
    public void setProductName(String n) { this.productName = n; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }

    public double getPrice() { return price; }
    public void setPrice(double p) { this.price = p; }
}