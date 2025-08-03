package com.luke;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class OrderbookTest {
    private Orderbook orderbook;

    @BeforeEach
    void setUp() {
        orderbook = new Orderbook();
    }

    // Helpers

    ArrayList<Trade> addBid(double price, int quantity) {
        return orderbook.addOrder(
            new Order(OrderType.GOOD_TILL_CANCELLED, Side.BUY, price, quantity)
        );
    }

    ArrayList<Trade> addAsk(double price, int quantity) {
        return orderbook.addOrder(
            new Order(OrderType.GOOD_TILL_CANCELLED, Side.SELL, price, quantity)
        );
    }

    // Adding orders

    @Test
    void testNegativePriceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
        new Order(OrderType.GOOD_TILL_CANCELLED, Side.BUY, -10.0, 5);
    });
}

    @Test
    void testAddGoodTillCancelledOrder() {
        Order order = new Order(OrderType.GOOD_TILL_CANCELLED, Side.BUY, 100.0, 10);
        ArrayList<Trade> trades = orderbook.addOrder(order);
        assertTrue(trades.isEmpty());
        assertEquals(1, orderbook.calculateOrderBookLevelInfos().getBids().size());
    }

    @Test
    void testAdddingOrderMultipleTimes() {
        Order order1 = new Order(OrderType.GOOD_TILL_CANCELLED, Side.BUY, 100.0, 10);
        orderbook.addOrder(order1);
        assertEquals(1, orderbook.calculateOrderBookLevelInfos().getBids().size());
        orderbook.addOrder(order1);
        assertEquals(1, orderbook.calculateOrderBookLevelInfos().getBids().size()); 
    }

    @Test
    public void testPriceTimePriorityExecution() {
        // Add two sell orders at the same price, different timestamps (earlier one first)

        addAsk(100, 10); // order 1
        addAsk(100, 10); // order 2


        // Add a sell order with a better price (should match first despite being newer)
        addAsk(90, 10);

        // Add a buy order that can match with all three

        ArrayList<Trade> trades = addBid(101, 30);

        // Verify number of trades
        assertEquals(3, trades.size(), "Should have executed 3 trades total.");

        // Verify execution order follows price-time priority
        assertEquals(2, trades.get(0).getAskTrade().getOrderId(), "Best price (order 3) should execute first.");
        assertEquals(0, trades.get(1).getAskTrade().getOrderId(), "Older order at 100.0 should execute before newer one.");
        assertEquals(1, trades.get(2).getAskTrade().getOrderId(), "Newer order at 100.0 should execute last.");
    }

    // Matching Orders

    @Test
    void testMatchOrdersExecutesTrade() {
        addBid(100.0, 10);
        ArrayList<Trade> trades = addAsk(100.0, 5);
        assertEquals(1, trades.size());
        assertEquals(5, trades.get(0).getQuantity());
    }

    @Test
    void testMatchOrdersStopsWhenBestBidBelowAsk() {
        addBid(99.0, 10);
        addAsk(101.0, 5);

        ArrayList<Trade> trades = orderbook.matchOrders();
        assertTrue(trades.isEmpty());
    }

    @Test
    void testPartialFillLeavesRemainder() {
        addBid( 100.0, 10);

        ArrayList<Trade> trades = addAsk(100.0, 20);
        assertEquals(1, trades.size());
        assertEquals(10, trades.get(0).getQuantity());

        assertEquals(1, orderbook.calculateOrderBookLevelInfos().getAsks().size());
        assertEquals(10, orderbook.calculateOrderBookLevelInfos().getAsks().get(0).getQuantity());
    }

    // Different Order Types

    @Test
    void testMarketOrderMatchesImmediately() {
        addAsk(100.0, 10);
        Order marketBuy = new Order(OrderType.MARKET, Side.BUY, 0, 10);
        ArrayList<Trade> trades = orderbook.addOrder(marketBuy);
        assertEquals(1, trades.size());
        assertEquals(10, trades.get(0).getQuantity());
    }

    @Test
    void testImmediateOrCancelDoesNotRestInBook() {
        addAsk(100.0, 5);
        Order ioc = new Order(OrderType.IMMEDIATE_OR_CANCEL, Side.BUY, 100.0, 10);
        orderbook.addOrder(ioc);
        assertEquals(0, orderbook.calculateOrderBookLevelInfos().getBids().size());
    }

    @Test
    void testMarketOrderDoesNotRestInBook() {
        addAsk(100.0, 5);
        Order ioc = new Order(OrderType.MARKET, Side.BUY, 0, 10);
        orderbook.addOrder(ioc);
        assertEquals(0, orderbook.calculateOrderBookLevelInfos().getBids().size());
    }

    @Test
    void testFillOrKillDoesNotRestInBook() {
        addAsk(100.0, 5);
        Order ioc = new Order(OrderType.FILL_OR_KILL, Side.BUY, 100.0, 10);
        orderbook.addOrder(ioc);
        assertEquals(0, orderbook.calculateOrderBookLevelInfos().getBids().size());
    }

    @Test
    void testFillOrKillCancelsIfCannotFullyMatch() {
        addAsk(100.0, 5);
        Order fok = new Order(OrderType.FILL_OR_KILL, Side.BUY, 100.0, 10);
        ArrayList<Trade> trades = orderbook.addOrder(fok);
        assertTrue(trades.isEmpty());
        assertEquals(0, orderbook.calculateOrderBookLevelInfos().getBids().size());
    }

    @Test
    void testFillOrKillExecutesIfEnoughLiquidity() {
        addAsk(100.0, 5);
        addAsk(100.0, 5);
        Order fok = new Order(OrderType.FILL_OR_KILL, Side.BUY, 100.0, 10);
        ArrayList<Trade> trades = orderbook.addOrder(fok);
        assertEquals(2, trades.size());
        assertEquals(10, trades.stream().mapToInt(Trade::getQuantity).sum());
    }

    // Cancel and Modify Orders

    @Test
    void testCancelOrderRemovesIt() {
        addBid(100.0, 10);
        orderbook.cancelOrder(0);
        assertEquals(0, orderbook.calculateOrderBookLevelInfos().getBids().size());
    }

    @Test
    void testModifyOrderChangesQuantity() {
        addBid(100.0, 10);
        OrderModify mod = new OrderModify(0, OrderType.GOOD_TILL_CANCELLED, 100.0, Side.BUY, 20);
        orderbook.modifyOrder(mod);
        assertEquals(20, orderbook.calculateOrderBookLevelInfos().getBids().get(0).getQuantity());
    }

    @Test
    void testModifyOrderInvalidIdDoesNothing() {
        OrderModify mod = new OrderModify(999,  OrderType.GOOD_TILL_CANCELLED, 100.0, Side.SELL, 20);
        orderbook.modifyOrder(mod);
        assertEquals(0, orderbook.calculateOrderBookLevelInfos().getAsks().size());
    }

    // OrderBook Info

    @Test
    void testOrderBookLevelInfosAggregatesQuantities() {
        addBid(100.0, 5);
        addBid(100.0, 10);
        addAsk(101.0, 7);

        OrderBookLevelInfos infos = orderbook.calculateOrderBookLevelInfos();
        assertEquals(15, infos.getBids().get(0).getQuantity());
        assertEquals(7, infos.getAsks().get(0).getQuantity());
    }

    // Concurrency 

    @Test
    void testConcurrentOrderAdds() throws InterruptedException {
        int threadCount = 10;
        int ordersPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < ordersPerThread; j++) {
                    addBid(100.0, 1);
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        OrderBookLevelInfos infos = orderbook.calculateOrderBookLevelInfos();
        assertEquals(threadCount * ordersPerThread, infos.getBids().get(0).getQuantity());
    }

    @Test
    void testConcurrentMatchingOrders() throws InterruptedException {
        int threadCount = 5;
        int ordersPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
    
        // Buyers
        for (int i = 0; i < threadCount; i++) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < ordersPerThread; j++) {
                        addBid(100.0, 1);
                    }
                    latch.countDown();
                }
            });
        }
    
        // Sellers
        for (int i = 0; i < threadCount; i++) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < ordersPerThread; j++) {
                        addAsk(100.0, 1);
                    }
                    latch.countDown();
                }
            });
        }
    
        latch.await();
        executor.shutdown();
    
        OrderBookLevelInfos infos = orderbook.calculateOrderBookLevelInfos();
        int remainingOrders = 0;
        for (int i = 0; i < infos.getBids().size(); i++) {
            remainingOrders += infos.getBids().get(i).getQuantity();
        }
        for (int i = 0; i < infos.getAsks().size(); i++) {
            remainingOrders += infos.getAsks().get(i).getQuantity();
        }
    
        assertTrue(remainingOrders <= 5, "Expected most orders to be matched");
    }

    @Test
    void testConcurrentCancellationsAndModifications() throws InterruptedException {
        // Add initial orders
        for (int i = 0; i < 50; i++) {
            addBid(100.0, 10);
        }

        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int orderId = i;
            executor.submit(() -> {
                try {
                    if (orderId % 2 == 0) {
                        orderbook.cancelOrder(orderId);
                    } else {
                        orderbook.modifyOrder(new OrderModify(orderId, OrderType.GOOD_TILL_CANCELLED, 100.0, Side.BUY, 20));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Validate that no negative quantities exist and the order book is in a consistent state
        int totalBidQty = orderbook.calculateOrderBookLevelInfos()
        .getBids()
        .stream()
        .mapToInt(info -> info.getQuantity())
        .sum();

        assertTrue(totalBidQty >= 0);
    }

}
