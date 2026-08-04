package com.ameyagidh.order.service;

import com.ameyagidh.order.client.InventoryClient;
import com.ameyagidh.order.model.Order;
import com.ameyagidh.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository repository, InventoryClient inventoryClient) {
        this.repository = repository;
        this.inventoryClient = inventoryClient;
    }

    public Order placeOrder(String sku, int quantity) {
        boolean reserved = inventoryClient.reserveStock(sku, quantity);
        String status = reserved ? "CONFIRMED" : "REJECTED_INSUFFICIENT_STOCK";
        Order order = new Order(sku, quantity, status);
        return repository.save(order);
    }

    public Iterable<Order> findAll() {
        return repository.findAll();
    }
}
