package com.luke;

public class Trade {

    private TradeInfo bidTrade;
    private TradeInfo askTrade;
    private int quantity;

    public TradeInfo getBidTrade() {
        return bidTrade;
    }
    public TradeInfo getAskTrade() {
        return askTrade;
    }

    public int getQuantity() {
        return quantity;
    }

    public Trade(TradeInfo bidTrade, TradeInfo askTrade, int quantity) {
        this.bidTrade = bidTrade;
        this.askTrade = askTrade;
        this.quantity = quantity;
    }

    public static Trade createTradeFromOrders(Order bid, Order ask, int quantity) {
        return new Trade(new TradeInfo(bid.getOrderId(), bid.getPrice()),
        new TradeInfo(ask.getOrderId(), ask.getPrice()),
        quantity);
    }
    
}
