package com.luke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

public class Orderbook {
    private TreeMap<Double, Queue<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    private TreeMap<Double, Queue<Order>> asks = new TreeMap<>();
    private HashMap<Integer, Order> orders = new HashMap<>();

    public boolean canFullyMatch(Order order) {
        TreeMap<Double, Queue<Order>> book = (order.getSide() == Side.BUY) ? this.asks : this.bids;
        int remainingQuantity = order.getRemainingQuantity();
        double targetPrice = order.getPrice();
        int totalAvailable = 0;

        for (Map.Entry<Double, Queue<Order>> entry : book.entrySet()) {
            double existingPrice = entry.getKey();
            boolean priceCompatible = (order.getSide() == Side.BUY) ? (targetPrice >= existingPrice) : (targetPrice <= existingPrice);
            if (!priceCompatible) break; 

            for (Order existingOrder : entry.getValue()) {
                totalAvailable += existingOrder.getRemainingQuantity();
                if (totalAvailable >= remainingQuantity) return true;
            }
        }
        return false;
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

            trades.add(Trade.createTradeFromOrders(bid, ask, quantity));

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
        return trades;
    }

    private void cleanUpEmptyLevels(Queue<Order> bidQueue, double bidPrice, Queue<Order> askQueue, double askPrice) {
        if (bidQueue.isEmpty()) {
                this.bids.remove(bidPrice);
            }

            if (askQueue.isEmpty()) {
                this.asks.remove(askPrice);
            }
    }

    private void addOrderHelper(Order order) {
        if (order.getSide() == Side.BUY) {
            Queue<Order> bidsAtPrice = this.bids.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()); // Or putIfAbsent?
            bidsAtPrice.add(order);
        }
        else {
            Queue<Order> asksAtPrice = this.asks.computeIfAbsent(order.getPrice(), k -> new LinkedList<>());
            asksAtPrice.add(order);
        }

        this.orders.put(order.getOrderId(), order);
    }

    public void addOrder(Order order) {
        if (this.orders.containsKey(order.getOrderId())) {
            System.err.println("Order with this id already exists.");
            return; 
        }

        if (order.getOrderType() == OrderType.FillOrKill) {
            if (!canFullyMatch(order)) {
                return;
            }
            addOrderHelper(order);
            matchOrders();
            return;
        }

        addOrderHelper(order);
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

    public OrderBookLevelInfos calculateOrderBookLevelInfos() {
        ArrayList<LevelInfo> bidInfos = new ArrayList<>();
        ArrayList<LevelInfo> askInfos = new ArrayList<>();

        bidInfos.ensureCapacity(this.bids.size());
        askInfos.ensureCapacity(this.asks.size());

        for (Map.Entry<Double, Queue<Order>> entry : this.bids.entrySet()) {
            Double price = entry.getKey();
            Queue<Order> ordersAtLevel = entry.getValue();
            bidInfos.add(createLevelInfo(price, ordersAtLevel));
        }

       for (Map.Entry<Double, Queue<Order>> entry : this.asks.entrySet()) {
            Double price = entry.getKey();
            Queue<Order> ordersAtLevel = entry.getValue();
            askInfos.add(createLevelInfo(price, ordersAtLevel));
        } 

        return new OrderBookLevelInfos(bidInfos, askInfos);

    }

    private LevelInfo createLevelInfo(double price, Queue<Order> ordersAtLevel) {
        int currentSum = 0;
        for (Order order : ordersAtLevel) {
            currentSum += order.getRemainingQuantity();
        }

        return new LevelInfo(price, currentSum);
    }
}
