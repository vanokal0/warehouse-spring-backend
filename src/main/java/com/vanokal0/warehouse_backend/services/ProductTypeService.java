package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.model.ProductType;
import com.vanokal0.warehouse_backend.repositories.ProductTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductTypeService {

    private final ProductTypeRepository productTypeRepository;

    @Autowired
    public ProductTypeService(ProductTypeRepository productTypeRepository) {
        this.productTypeRepository = productTypeRepository;
    }

    public List<String> getAllTypeNames() {
        return productTypeRepository.findAll().stream().map(ProductType::getTypeName).toList();
    }
}
