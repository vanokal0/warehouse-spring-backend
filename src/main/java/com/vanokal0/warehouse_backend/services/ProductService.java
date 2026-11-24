package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.model.Product;
import com.vanokal0.warehouse_backend.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository repository) {
        this.productRepository = repository;
    }

    public List<String> getAllProductNames() {
        return productRepository.findAll()
                .stream()
                .map(Product::getProductName)
                .toList();
    }

    public String getTypeByProductName(String name) {
        Product product = productRepository.findByProductName(name)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return product.getProductType().getTypeName();
    }
}
