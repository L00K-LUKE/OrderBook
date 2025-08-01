package com.luke;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {

     @BeforeEach
    void resetOrderIdIncrementor() throws Exception {
        // Use reflection to reset the static counter for predictable tests
        var field = Order.class.getDeclaredField("OrderIdIncrementor");
        field.setAccessible(true);
        field.set(null, 0);
    }

    @Test
    void testValidOrderCreation() {
        Order order = new Order(OrderType.GOOD_TILL_CANCELLED, Side.BUY, 100.5, 10);

        assertEquals(0, order.getOrderId());
        assertEquals(OrderType.GOOD_TILL_CANCELLED, order.getOrderType());
        assertEquals(Side.BUY, order.getSide());
        assertEquals(100.5, order.getPrice());
        assertEquals(10, order.getInitialQuantity());
        assertEquals(10, order.getRemainingQuantity());
        assertFalse(order.isFilled());
    }

    @Test
    void testOrderIdIncrementsAcrossOrders() {
        Order order1 = new Order(OrderType.MARKET, Side.SELL, 50, 5);
        Order order2 = new Order(OrderType.FILL_OR_KILL, Side.BUY, 60, 10);

        assertEquals(0, order1.getOrderId());
        assertEquals(1, order2.getOrderId());
    }

    @Test
    void testNegativePriceThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order(OrderType.MARKET, Side.BUY, -10, 5));
    }

    @Test
    void testZeroOrNegativeQuantityThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order(OrderType.GOOD_TILL_CANCELLED, Side.SELL, 10, 0));

        assertThrows(IllegalArgumentException.class,
                () -> new Order(OrderType.MARKET, Side.SELL, 10, -5));
    }

    @Test
    void testFillReducesRemainingQuantity() {
        Order order = new Order(OrderType.IMMEDIATE_OR_CANCEL, Side.BUY, 100, 10);

        order.fill(4);
        assertEquals(6, order.getRemainingQuantity());
        assertEquals(4, order.getFilledQuantity());
        assertFalse(order.isFilled());

        order.fill(6);
        assertEquals(0, order.getRemainingQuantity());
        assertTrue(order.isFilled());
    }

    @Test
    void testFillMoreThanRemainingThrowsException() {
        Order order = new Order(OrderType.MARKET, Side.BUY, 100, 10);

        assertThrows(IllegalArgumentException.class, () -> order.fill(11));
    }
}
