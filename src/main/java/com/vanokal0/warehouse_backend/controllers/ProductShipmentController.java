package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.ProductPlacementDTO;
import com.vanokal0.warehouse_backend.dto.ProductShipmentDTO;
import com.vanokal0.warehouse_backend.services.ProductShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-shipments")
public class ProductShipmentController {

    private final ProductShipmentService shipmentService;

    @Autowired
    public ProductShipmentController(ProductShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/by-order-and-product")
    public List<ProductShipmentDTO> getShipmentsByOrderAndProduct(
            @RequestParam String orderNumber,
            @RequestParam String productName
    ) {
        return shipmentService.getByOrderAndProduct(orderNumber, productName);
    }

    @PostMapping
    public ResponseEntity<?> createShipment(@RequestBody ProductShipmentDTO dto) {
        try{
            return ResponseEntity.ok(shipmentService.createShipment(dto));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка створення відвантаження: " + e.getMessage());
        }

    }

    @PutMapping("/{number}")
    public ResponseEntity<?> updateShipment(@PathVariable String number, @RequestBody ProductShipmentDTO dto) {
        try{
            return ResponseEntity.ok(shipmentService.updateShipment(number, dto));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка оновлення відвантаження: " + e.getMessage());
        }
    }

    @DeleteMapping("/{number}")
    public ResponseEntity<?> deleteShipment(@PathVariable String number){
        try{
            shipmentService.deleteShipment(number);
            return ResponseEntity.noContent().build();
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка видалення відвантаження: " + e.getMessage());
        }

    }
}
