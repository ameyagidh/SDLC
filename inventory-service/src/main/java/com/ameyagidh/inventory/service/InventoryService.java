package com.ameyagidh.inventory.service;

import com.ameyagidh.inventory.model.Product;
import com.ameyagidh.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class InventoryService {

    private final ProductRepository repository;

    public InventoryService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public Product findBySku(String sku) {
        return repository.findBySku(sku)
                .orElseThrow(() -> new NoSuchElementException("No product with sku " + sku));
    }

    public Product create(Product product) {
        return repository.save(product);
    }

    public synchronized boolean reserveStock(String sku, int quantity) {
        Product product = findBySku(sku);
        if (product.getQuantityAvailable() < quantity) {
            return false;
        }
        product.setQuantityAvailable(product.getQuantityAvailable() - quantity);
        repository.save(product);
        return true;
    }
}
