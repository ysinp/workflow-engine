package com.example.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A row in the banking product catalog shown on the sign-up wizard's product-selection screen.
 * Presentational only - see ProductSelect.tsx: whichever product a customer picks, the same KYC
 * wizard and account-creation flow runs underneath. {@code displayOrder} preserves catalog order
 * (ordinary database row order is not guaranteed), matching the order the product team curated.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Column(nullable = false)
    private int displayOrder;

    protected Product() {
        // JPA
    }

    public Product(String name, String description, ProductCategory category, int displayOrder) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
