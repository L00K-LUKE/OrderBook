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
        TreeMap<Double, Queue<Order>> existingOrders = (order.getSide() == Side.BUY) ? this.bids : this.asks;
        Queue<Order> queue = existingOrders.computeIfAbsent(order.getPrice(), k -> new LinkedList<>());
        queue.add(order);
        this.orders.put(order.getOrderId(), order);
    }

    public ArrayList<Trade> addOrder(Order order) {
        ArrayList<Trade> trades = new ArrayList<>();

        if (this.orders.containsKey(order.getOrderId())) {
            System.err.println("Order with this id already exists.");
            return trades; 
        }

        switch (order.getOrderType()) {
            case FillOrKill:
                if (!canFullyMatch(order)) {
                    return trades;
                }
                addOrderHelper(order);
                trades.addAll(matchOrders());
                break;

            case GoodTillCancelled:
                addOrderHelper(order);
                trades.addAll(matchOrders());
                break;
            
            case ImmediateOrCancel:
                addOrderHelper(order);
                trades.addAll(matchOrders());
                if (!order.isFilled()) {
                    cancelOrder(order.getOrderId());
                }
                break;

            case Market:
                trades.addAll(executeMarketOrder(order)); 
                break;
            default:
                break;
        }
        return trades;
    }

    private ArrayList<Trade> executeMarketOrder(Order order) {
        ArrayList<Trade> trades = new ArrayList<>();

        TreeMap<Double, Queue<Order>> book = (order.getSide() == Side.BUY) ? this.asks : this.bids;
        int remainingQuantity = order.getRemainingQuantity();

        while (remainingQuantity > 0 && !book.isEmpty()) {
            Map.Entry<Double, Queue<Order>> bestLevel = book.firstEntry();
            Queue<Order> queue = bestLevel.getValue();
            Order matchedWithOrder = queue.peek();

            int quantity = Math.min(remainingQuantity, matchedWithOrder.getRemainingQuantity());
            matchedWithOrder.fill(quantity);
            order.fill(quantity);

            Order bidOrder = (order.getSide() == Side.BUY) ? order : matchedWithOrder;
            Order askOrder = (order.getSide() == Side.SELL) ? order : matchedWithOrder;
            trades.add(Trade.createTradeFromOrders(bidOrder, askOrder, quantity));

            remainingQuantity -= quantity;

            if (matchedWithOrder.isFilled()) {
                queue.poll();
                orders.remove(matchedWithOrder.getOrderId());
            }

            if (queue.isEmpty()) {
                book.remove(bestLevel.getKey());
            }
        }
        return trades;
    }

    public void cancelOrder(int orderId) {
        Order orderToDelete = this.orders.remove(orderId);
        if (orderToDelete == null) return;

        TreeMap<Double, Queue<Order>> bookSide = (orderToDelete.getSide() == Side.BUY) ? this.bids : this.asks;
        double price = orderToDelete.getPrice();
        Queue<Order> queue = bookSide.get(price);

        if (queue == null) return;

        queue.remove(orderToDelete);
        if (queue.isEmpty()) bookSide.remove(price);
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
