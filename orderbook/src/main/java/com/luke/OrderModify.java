package com.luke;

public class OrderModify {

    private int orderId;
    private OrderType orderType;
    private double price;
    private Side side;
    private int quantity;

    public OrderModify(int orderId, OrderType orderType, double price, Side side, int quantity) {
        this.orderId = orderId;
        this.orderType = orderType;
        this.price = price;
        this.side = side;
        this.quantity = quantity;
    }

    public int getOrderId() {
        return orderId;
    }

    public OrderType getOrderType() {
        return orderType;
    }
    public double getPrice() {
        return price;
    }
    public Side getSide() {
        return side;
    }
    public int getQuantity() {
        return quantity;
    }

    public Order createOrder() {
        return new Order(orderType, side, price, quantity);
    }
}
