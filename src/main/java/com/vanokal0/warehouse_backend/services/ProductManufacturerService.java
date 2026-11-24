package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.model.ProductManufacturer;
import com.vanokal0.warehouse_backend.repositories.ProductManufacturerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductManufacturerService {

    private final ProductManufacturerRepository repository;

    @Autowired
    public ProductManufacturerService(ProductManufacturerRepository repository) {
        this.repository = repository;
    }

    public List<String> getAllManufacturerNames() {
        return repository.findAll()
                .stream()
                .map(ProductManufacturer::getCompanyName)
                .toList();
    }
}
