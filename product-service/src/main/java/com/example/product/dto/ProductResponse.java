package com.example.product.dto;

import com.example.product.entity.Product;

public record ProductResponse(Long id, String name, String description, String category) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getCategory().name());
    }
}
