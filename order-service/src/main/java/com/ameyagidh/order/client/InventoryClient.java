package com.ameyagidh.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder,
                            @Value("${inventory.service.url}") String inventoryServiceUrl) {
        this.restClient = builder.baseUrl(inventoryServiceUrl).build();
    }

    public boolean reserveStock(String sku, int quantity) {
        try {
            restClient.post()
                    .uri("/api/inventory/reserve")
                    .body(new ReserveRequest(sku, quantity))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.Conflict e) {
            return false;
        }
    }

    public record ReserveRequest(String sku, int quantity) {
    }
}
