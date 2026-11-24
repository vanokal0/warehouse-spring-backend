package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.SupplierDTO;
import com.vanokal0.warehouse_backend.dto.WarehouseDTO;
import com.vanokal0.warehouse_backend.services.SupplierService;
import com.vanokal0.warehouse_backend.services.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService supplierService;
    @Autowired
    public SupplierController(SupplierService supplierService){
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<List<SupplierDTO>> getAllSuppliers(){
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/names")
    public ResponseEntity<List<String>> getAllCompanyNames() {
        return ResponseEntity.ok(supplierService.getAllSupplierNames());
    }
}