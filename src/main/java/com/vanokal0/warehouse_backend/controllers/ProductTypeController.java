package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.services.ProductTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-types")
public class ProductTypeController {

    private final ProductTypeService productTypeService;

    @Autowired
    public ProductTypeController(ProductTypeService productTypeService) {
        this.productTypeService = productTypeService;
    }

    @GetMapping("/type-names")
    public ResponseEntity<List<String>> getAllTypeNames() {
        return ResponseEntity.ok(productTypeService.getAllTypeNames());
    }
}
