package com.example.product.config;

import com.example.product.entity.Product;
import com.example.product.entity.ProductCategory;
import com.example.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.product.entity.ProductCategory.BUSINESS;
import static com.example.product.entity.ProductCategory.PERSONAL;

/** Seeds the demo product catalog once, if the table is empty - mirrors create-account's ReviewerAccountSeeder. */
@Component
public class ProductCatalogSeeder implements CommandLineRunner {

    private final ProductRepository repository;

    public ProductCatalogSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        List<Product> catalog = List.of(
                product("Elite Pay Personal Checking", "Indulge in exclusivity.", PERSONAL, 0),
                product("FutureSaver Personal Savings", "Grow with us.", PERSONAL, 1),
                product("Retirement Savings", "Secure your golden years.", PERSONAL, 2),
                product("InstaLoan", "Loans simplified.", PERSONAL, 3),
                product("Revolving Line of Credit", "Fuel your cash flow.", PERSONAL, 4),
                product("Spark Business Checking", "Empower your business.", BUSINESS, 5),
                product("Evergreen Business Savings", "Secure your business future.", BUSINESS, 6),
                product("Certificate of Deposit", "Secured growth, short term.", BUSINESS, 7),
                product("Business Revolving Line Of Credit", "Fuel your cash flow.", BUSINESS, 8),
                product("Premium Checking", "Experience the advantage.", PERSONAL, 9),
                product("Elevate Personal Credit Card", "Every reward personalized for you.", PERSONAL, 10),
                product("Whole Life Insurance", "Secure today, assured tomorrow.", PERSONAL, 11),
                product("Premium Savings", "Elevate your savings.", PERSONAL, 12),
                product("Propel Business Credit Card", "Unleash your business potential.", BUSINESS, 13),
                product("Certificate of Deposit", "Secured growth, short term.", PERSONAL, 14)
        );

        repository.saveAll(catalog);
    }

    private static Product product(String name, String description, ProductCategory category, int displayOrder) {
        return new Product(name, description, category, displayOrder);
    }
}
