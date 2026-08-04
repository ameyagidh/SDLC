package com.ameyagidh.inventory;

import com.ameyagidh.inventory.model.Product;
import com.ameyagidh.inventory.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository repository;

    public DataSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        repository.save(new Product("SKU-WIDGET-001", "Widget", 100, 9.99));
        repository.save(new Product("SKU-GADGET-002", "Gadget", 50, 24.99));
        repository.save(new Product("SKU-GIZMO-003", "Gizmo", 5, 149.99));
    }
}
