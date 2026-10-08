package com.example.product.controller;

import com.example.product.dto.ProductResponse;
import com.example.product.entity.ProductCategory;
import com.example.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void list_withNoCategory_returnsFullCatalogInServiceOrder() throws Exception {
        when(productService.list(isNull())).thenReturn(List.of(
                new ProductResponse(1L, "Elite Pay Personal Checking", "Indulge in exclusivity.", "PERSONAL"),
                new ProductResponse(2L, "Spark Business Checking", "Empower your business.", "BUSINESS")));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Elite Pay Personal Checking"))
                .andExpect(jsonPath("$[0].category").value("PERSONAL"))
                .andExpect(jsonPath("$[1].name").value("Spark Business Checking"))
                .andExpect(jsonPath("$[1].category").value("BUSINESS"));
    }

    @Test
    void list_withCategory_returnsOnlyThatCategory() throws Exception {
        when(productService.list(ProductCategory.PERSONAL)).thenReturn(List.of(
                new ProductResponse(1L, "Elite Pay Personal Checking", "Indulge in exclusivity.", "PERSONAL")));

        mockMvc.perform(get("/api/products").param("category", "PERSONAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("PERSONAL"));
    }
}
