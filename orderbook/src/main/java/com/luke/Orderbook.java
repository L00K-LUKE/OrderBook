package com.luke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Level;

public class Orderbook {
    private TreeMap<Double, Queue<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    private TreeMap<Double, Queue<Order>> asks = new TreeMap<>();
    private HashMap<Integer, Order> orders = new HashMap<>();

    public boolean canMatch(Side side, double price) {
        if (side.equals(Side.BUY)) {
            if (this.asks.isEmpty()) {
                return false;
            }
            double lowestAsk = this.asks.firstKey();

            return lowestAsk <= price;
        }

        else {
            if (this.bids.isEmpty()) {
                return false;
            }
            double highestBid = this.bids.firstKey();
            return highestBid >= price;
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
                orders.remove(bid.getOrderId());
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

    public void addOrder(Order order) {
        if (this.orders.containsKey(order.getOrderId())) {
            System.err.println("Order with this id already exists.");
            return; 
        }

        if (!canMatch(order.getSide(), order.getPrice()) && order.getOrderType() == OrderType.FillAndKill) {
            return;
        }

        if (order.getSide() == Side.BUY) {
            Queue<Order> bidsAtPrice = this.bids.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()); // Or putIfAbsent?
            bidsAtPrice.add(order);
        }
        else {
            Queue<Order> asksAtPrice = this.asks.computeIfAbsent(order.getPrice(), k -> new LinkedList<>());
            asksAtPrice.add(order);
        }

        this.orders.put(order.getOrderId(), order);
        return;
    }

    public void cancelOrder(int orderId) {
        Order orderToDelete = this.orders.remove(orderId);

        if (orderToDelete == null) {
            return;
        }

        double price = orderToDelete.getPrice();
        Queue<Order> queue;

        if (orderToDelete.getSide() == Side.BUY) {
            queue = this.bids.get(price);
            if (queue == null) {
                return;
            }

            queue.remove(orderToDelete);
            if (queue.isEmpty()) {
                this.bids.remove(price);
            }
            
        } else {
            queue = this.asks.get(price);
            if (queue == null) {
                return;
            }

            queue.remove(orderToDelete);
            if (queue.isEmpty()) {
                this.asks.remove(price);
            }
        }
    }

    public void modifyOrder(OrderModify replacement) {
        if (!this.orders.containsKey(replacement.getOrderId())) {
            System.err.println("No order exists with that OrderID");
            return;
        }

        this.cancelOrder(replacement.getOrderId());
        this.addOrder(replacement.createOrder());
    }

    public OrderBookLevelInfos getOrderBookLevelInfos() {
        ArrayList<LevelInfo> bidInfos = new ArrayList<>();
        ArrayList<LevelInfo> askInfos = new ArrayList<>();

        bidInfos.ensureCapacity(this.bids.size());
        askInfos.ensureCapacity(this.asks.size());

        for (Map.Entry<Double, Queue<Order>> entry : this.bids.entrySet()) {
            Double price = entry.getKey();
            Queue<Order> ordersAtLevel = entry.getValue();
            bidInfos.add(creatLevelInfo(price, ordersAtLevel));
        }

       for (Map.Entry<Double, Queue<Order>> entry : this.asks.entrySet()) {
            Double price = entry.getKey();
            Queue<Order> ordersAtLevel = entry.getValue();
            askInfos.add(creatLevelInfo(price, ordersAtLevel));
        } 

        return new OrderBookLevelInfos(bidInfos, askInfos);

    }

    private LevelInfo creatLevelInfo(double price, Queue<Order> ordersAtLevel) {
        int currentSum = 0;
        for (Order order : ordersAtLevel) {
            currentSum += order.getRemainingQuantity();
        }

        return new LevelInfo(price, currentSum);
    }
}
