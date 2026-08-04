package com.ameyagidh.order.controller;

import com.ameyagidh.order.client.InventoryClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryClient inventoryClient;

    @Test
    void listOrdersReturnsOk() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk());
    }

    @Test
    void placeOrderConfirmsWhenStockAvailable() throws Exception {
        when(inventoryClient.reserveStock("SKU-ABC", 3)).thenReturn(true);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-ABC\",\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void placeOrderRejectsWhenStockUnavailable() throws Exception {
        when(inventoryClient.reserveStock("SKU-XYZ", 999)).thenReturn(false);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-XYZ\",\"quantity\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED_INSUFFICIENT_STOCK"));
    }
}
