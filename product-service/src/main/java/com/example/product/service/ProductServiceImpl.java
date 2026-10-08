package com.example.product.service;

import com.example.product.dto.ProductResponse;
import com.example.product.entity.Product;
import com.example.product.entity.ProductCategory;
import com.example.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;

    public ProductServiceImpl(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductResponse> list(ProductCategory category) {
        List<Product> products = category == null
                ? repository.findAllByOrderByDisplayOrderAsc()
                : repository.findAllByCategoryOrderByDisplayOrderAsc(category);
        return products.stream().map(ProductResponse::from).toList();
    }
}
