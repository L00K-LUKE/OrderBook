package com.luke;

public class Order {
    
    private static int OrderIdIncrementor = 0;

    private int OrderId;
    private OrderType orderType;
    private Side side;
    private double price;
    private int initialQuantity;
    private int remainingQuantity;

    public Order(OrderType orderType, Side side, double price, int quantity) {
        this.OrderId = Order.OrderIdIncrementor++;
        this.orderType = orderType;
        this.side = side;
        this.price = price;
        this.initialQuantity = quantity;
        this.remainingQuantity = quantity;
    }

    public int getOrderId() {
        return OrderId;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public Side getSide() {
        return side;
    }

    public double getPrice() {
        return price;
    }

    public int getInitialQuantity() {
        return initialQuantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public int getFilledQuantity() {
        return this.getInitialQuantity() - this.getRemainingQuantity();
    }

    public boolean isFilled() {
        return this.getRemainingQuantity() == 0;
    }

    public void fill(int quantity) {
        if (quantity > this.getRemainingQuantity()) {
            throw new IllegalArgumentException("Order " + this.getOrderId() + ", cannot be filled by more than its remaining quantity.");
        }
        this.remainingQuantity -= quantity;
    }
}
