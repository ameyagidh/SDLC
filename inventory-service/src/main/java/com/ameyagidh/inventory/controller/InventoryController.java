package com.ameyagidh.inventory.controller;

import com.ameyagidh.inventory.model.Product;
import com.ameyagidh.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public List<Product> listProducts() {
        return service.findAll();
    }

    @GetMapping("/products/{sku}")
    public Product getProduct(@PathVariable("sku") String sku) {
        return service.findBySku(sku);
    }

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(product));
    }

    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserve(@RequestBody ReserveRequest request) {
        boolean reserved = service.reserveStock(request.sku(), request.quantity());
        if (!reserved) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("reserved", false, "reason", "insufficient stock"));
        }
        return ResponseEntity.ok(Map.of("reserved", true, "sku", request.sku(), "quantity", request.quantity()));
    }

    public record ReserveRequest(String sku, int quantity) {
    }
}
