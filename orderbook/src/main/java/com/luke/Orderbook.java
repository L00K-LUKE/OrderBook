package com.luke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Queue;
import java.util.TreeMap;

public class Orderbook {
    private TreeMap<Double, Queue<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    private TreeMap<Double, Queue<Order>> asks = new TreeMap<>();
    private HashMap<Integer, Order> orders = new HashMap<>();

    public boolean canMatch(Side side, double price) {
        if (side.equals(Side.BUY)) {
            if (this.bids.isEmpty()) {
                return false;
            }
            double lowestAsk = this.asks.firstKey();

            return lowestAsk <= price;
        }

        else if (side.equals(Side.SELL)) {
            if (this.bids.isEmpty()) {
                return false;
            }
            double highestBid = this.bids.firstKey();
            return highestBid >= price;
        }

        else {
            throw new IllegalArgumentException("Side must be of either buy or sell enum type");
        }

    }

    public ArrayList<Trade> matchOrders() {
        ArrayList<Trade> trades = new ArrayList<>();

        while (!this.bids.isEmpty() && !this.asks.isEmpty()) {
            double bestBidPrice = this.bids.firstKey();
            double bestAskPrice = this.asks.firstKey();

            if (bestBidPrice < bestAskPrice) {
                break;
            }

            Queue<Order> bidQueue = this.bids.get(bestBidPrice);
            Queue<Order> askQueue = this.asks.get(bestAskPrice);

            Order bid = bidQueue.peek();
            Order ask = askQueue.peek();

            int quantity = Math.min(bid.getRemainingQuantity(), ask.getRemainingQuantity());

            bid.fill(quantity);
            ask.fill(quantity);

            trades.add(Trade.create_trade_from_orders(bid, ask, quantity));

            if (bid.isFilled()) {
                bidQueue.poll();
                orders.remove(ask.getOrderId());
            }

            if (ask.isFilled()) {
                askQueue.poll();
                orders.remove(ask.getOrderId());
            }
            
            cleanUpEmptyLevels(bidQueue, bestBidPrice, askQueue, bestAskPrice);
            
        }

        removeFillOrKills();

        return trades;
    }

    private void removeFillOrKills() {
        for (Order order : this.orders.values()) {
            if (order.getOrderType().equals(OrderType.FillAndKill)) {
                this.cancelOrder(order.getOrderId());
            }
        }
    }

    private void cleanUpEmptyLevels(Queue<Order> bidQueue, double bidPrice, Queue<Order> askQueue, double askPrice) {
        if (bidQueue.isEmpty()) {
                this.bids.remove(bidPrice);
            }

            if (askQueue.isEmpty()) {
                this.asks.remove(askPrice);
            }
    }

    public void addTrade(Order order) {
        if (this.orders.containsKey(order.getOrderId())) {
            throw new IllegalArgumentException("Duplicate orderID"); 
        }

        if (canMatch(order.getSide(), order.getPrice()) {
            
            
        }
    }

}
