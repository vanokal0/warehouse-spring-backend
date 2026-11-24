package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.ProductPlacementDTO;
import com.vanokal0.warehouse_backend.services.ProductPlacementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-placements")
public class ProductPlacementController {

    private final ProductPlacementService placementService;

    @Autowired
    public ProductPlacementController(ProductPlacementService placementService) {
        this.placementService = placementService;
    }

    @GetMapping("/by-order-and-product")
    public List<ProductPlacementDTO> getPlacementsByOrderAndProduct(
            @RequestParam String orderNumber,
            @RequestParam String productName
    ) {
        return placementService.getByOrderAndProduct(orderNumber, productName);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductPlacementDTO dto) {

        try{
            return ResponseEntity.ok(placementService.createPlacement(dto));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка створення отримання: " + e.getMessage());
        }
    }

    @PutMapping("/{number}")
    public ResponseEntity<?> update(@PathVariable String number, @RequestBody ProductPlacementDTO dto) {
        try{
            return ResponseEntity.ok(placementService.updatePlacement(number, dto));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка створення отримання: " + e.getMessage());
        }

    }

    @DeleteMapping("/{number}")
    public ResponseEntity<Void> deletePlacement(@PathVariable String number){
        placementService.deletePlacement(number);
        return ResponseEntity.noContent().build();
    }
}
