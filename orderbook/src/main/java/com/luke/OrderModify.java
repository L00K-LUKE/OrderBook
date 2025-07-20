package com.luke;

public class OrderModify {

    private int orderId;
    private double price;
    private Side side;
    private int quantity;

    public OrderModify(int orderId, double price, Side side, int quantity) {
        this.orderId = orderId;
        this.price = price;
        this.side = side;
        this.quantity = quantity;
    }

    public int getOrderId() {
        return orderId;
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

    public Order toOrder(OrderType orderType) {
        return new Order(this.orderId, orderType, this.side, this.price, this.quantity);
    }
}
