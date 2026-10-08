package com.example.product.controller;

import com.example.product.dto.ProductResponse;
import com.example.product.entity.ProductCategory;
import com.example.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** Backs the sign-up wizard's product-selection screen; ?category=PERSONAL|BUSINESS narrows the list. */
    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) ProductCategory category) {
        return productService.list(category);
    }
}
