package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.ConcreteProductDTO;
import com.vanokal0.warehouse_backend.dto.OrderDTO;
import com.vanokal0.warehouse_backend.model.ConcreteProduct;
import com.vanokal0.warehouse_backend.services.ConcreteProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/concrete-products")
public class ConcreteProductController {
    private final ConcreteProductService concreteProductService;

    public ConcreteProductController(ConcreteProductService concreteProductService) {

        this.concreteProductService = concreteProductService;
    }

    @GetMapping
    public ResponseEntity<List<ConcreteProductDTO>> getAllConcreteProducts(){
        return ResponseEntity.ok(concreteProductService.getAllConcreteProducts());
    }

    @GetMapping("/paged")
    public ResponseEntity<Map<String, Object>> getPagedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) List<String> sortField,
            @RequestParam(required = false) List<String> sortOrder,
            @RequestParam(required = false) List<String> filterField,
            @RequestParam(required = false) List<String> filterOperator,
            @RequestParam(required = false) List<String> filterValue
    ) {
        return ResponseEntity.ok(concreteProductService.getPagedProducts(
                page, pageSize, sortField, sortOrder, filterField, filterOperator, filterValue
        ));
    }

    @PostMapping
    public ResponseEntity<?> createConcreteProduct(@RequestBody ConcreteProductDTO dto) {
        try {
            ConcreteProductDTO saved = concreteProductService.createConcreteProduct(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка при створенні продукту: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateConcreteProduct(
            @PathVariable Integer id,
            @RequestBody ConcreteProductDTO dto
    ) {
        try {
            return ResponseEntity.ok(concreteProductService.updateConcreteProduct(id, dto));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка при оновленні продукту: " + e.getMessage());
        }
    }

}
