package com.luke;

public class Trade {

    private TradeInfo bidTrade;
    private TradeInfo askTrade;

    public TradeInfo getBidTrade() {
        return bidTrade;
    }
    public TradeInfo getAskTrade() {
        return askTrade;
    }

    public Trade(TradeInfo bidTrade, TradeInfo askTrade) {
        this.bidTrade = bidTrade;
        this.askTrade = askTrade;
    }

    public Trade create_trade_from_orders(Order bid, Order ask, int quantity) {
        return new Trade(new TradeInfo(bid.getOrderId(), bid.getPrice(), quantity),
        new TradeInfo(ask.getOrderId(), ask.getPrice(), quantity));
    }
    
}
