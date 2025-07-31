package com.luke;

public class TradeInfo {
    private int orderId;
    private double price;

    public TradeInfo(int orderId, double price) {
        this.orderId = orderId;
        this.price = price;
    }

    public int getOrderId() {
        return orderId;
    }

    public double getPrice() {
        return price;
    }
}
