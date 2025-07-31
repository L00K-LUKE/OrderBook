package com.luke;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

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
        orderbook.cancelOrder(1);
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
}
