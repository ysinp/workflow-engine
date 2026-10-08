package com.example.product.service;

import com.example.product.dto.ProductResponse;
import com.example.product.entity.ProductCategory;

import java.util.List;

public interface ProductService {

    /** The catalog in curated display order, optionally narrowed to one category (null = all). */
    List<ProductResponse> list(ProductCategory category);
}
