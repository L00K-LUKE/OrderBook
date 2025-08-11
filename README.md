# OrderBook

This project is a Java-based orderbook designed to simulate a simplified financial exchange. It supports various order types, cancellations, and modifications, maintaining an efficient price-time priority order book for both bids and asks. The engine is built with thread safety in mind, making it capable of handling concurrent order submissions in a multi-threaded environment.

# Features

* Price-Time Priority Matching – Orders are matched based on price first, then arrival time.

* Efficient Data Structures – Uses TreeMap<Price, Queue<Order>> for O(log n) level lookups.

* Partial Order Fills – Handles cases where incoming orders are only partially matched.

* Order Modification and Cancellation – Supports live updates to existing orders.
  
  * I chose to implement this behaviour through the cancellation of the old order and submition of a new one, rather than modifying the existing order in place. This is to avoid complications like partially filled orders jumping between price levels, and old orders hogging the front of the queue by repeatedly increasing quantity thereby preventing new orders from executing.

* Concurrent Processing – Thread-safe design to handle simultaneous buyers and sellers.

* Comprehensive Unit Testing – Includes tests for matching, edge cases, and concurrency behavior.
