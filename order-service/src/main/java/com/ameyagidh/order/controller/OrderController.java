package com.ameyagidh.order.controller;

import com.ameyagidh.order.model.Order;
import com.ameyagidh.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public Iterable<Order> listOrders() {
        return service.findAll();
    }

    @PostMapping
    public Order placeOrder(@RequestBody PlaceOrderRequest request) {
        return service.placeOrder(request.sku(), request.quantity());
    }

    public record PlaceOrderRequest(String sku, int quantity) {
    }
}
