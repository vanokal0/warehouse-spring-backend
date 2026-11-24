package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService service) {
        this.productService = service;
    }

    @GetMapping("/names")
    public List<String> getAllProductNames() {
        return productService.getAllProductNames();
    }

    @GetMapping("/get-type-by-name/{name}")
    public ResponseEntity<String> getTypeByProductName(@PathVariable String name) {
        return ResponseEntity.ok(productService.getTypeByProductName(name));
    }
}
