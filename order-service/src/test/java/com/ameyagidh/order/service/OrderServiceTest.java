package com.ameyagidh.order.service;

import com.ameyagidh.order.client.InventoryClient;
import com.ameyagidh.order.model.Order;
import com.ameyagidh.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private InventoryClient inventoryClient;

    @Test
    void confirmsOrderWhenStockReserved() {
        when(inventoryClient.reserveStock("SKU-1", 2)).thenReturn(true);
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderService service = new OrderService(repository, inventoryClient);
        Order order = service.placeOrder("SKU-1", 2);

        assertEquals("CONFIRMED", order.getStatus());
    }

    @Test
    void rejectsOrderWhenStockNotReserved() {
        when(inventoryClient.reserveStock("SKU-2", 999)).thenReturn(false);
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderService service = new OrderService(repository, inventoryClient);
        Order order = service.placeOrder("SKU-2", 999);

        assertEquals("REJECTED_INSUFFICIENT_STOCK", order.getStatus());
    }
}
