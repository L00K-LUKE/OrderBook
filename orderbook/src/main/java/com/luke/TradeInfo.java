package com.luke;

public class TradeInfo {
    private int orderId;
    private double price;
    private int quantity;

    public TradeInfo(int orderId, double price, int quantity) {
        this.orderId = orderId;
        this.price = price;
        this.quantity = quantity;
    }

    public int getOrderId() {
        return orderId;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }
    
    
}
