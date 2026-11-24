package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.services.ProductManufacturerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manufacturers")
public class ProductManufacturerController {

    private final ProductManufacturerService service;

    @Autowired
    public ProductManufacturerController(ProductManufacturerService service) {
        this.service = service;
    }

    @GetMapping("/names")
    public List<String> getAllManufacturerNames() {
        return service.getAllManufacturerNames();
    }
}
