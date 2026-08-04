package com.ameyagidh.inventory.service;

import com.ameyagidh.inventory.model.Product;
import com.ameyagidh.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(new Product("SKU-TEST-001", "Test Widget", 10, 5.0));
    }

    @Test
    void reservesStockWhenAvailable() {
        boolean reserved = inventoryService.reserveStock("SKU-TEST-001", 4);

        assertTrue(reserved);
        assertEquals(6, inventoryService.findBySku("SKU-TEST-001").getQuantityAvailable());
    }

    @Test
    void rejectsReservationWhenInsufficientStock() {
        boolean reserved = inventoryService.reserveStock("SKU-TEST-001", 100);

        assertFalse(reserved);
        assertEquals(10, inventoryService.findBySku("SKU-TEST-001").getQuantityAvailable());
    }
}
