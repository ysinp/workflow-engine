package com.example.product.repository;

import com.example.product.entity.Product;
import com.example.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByDisplayOrderAsc();

    List<Product> findAllByCategoryOrderByDisplayOrderAsc(ProductCategory category);
}
