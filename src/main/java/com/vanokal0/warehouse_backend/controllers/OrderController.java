package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.OrderDTO;
import com.vanokal0.warehouse_backend.dto.SupplierDTO;
import com.vanokal0.warehouse_backend.model.Order;
import com.vanokal0.warehouse_backend.services.OrderService;
import com.vanokal0.warehouse_backend.services.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/paged")
    public ResponseEntity<Map<String, Object>> getPagedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) List<String> sortField,
            @RequestParam(required = false) List<String> sortOrder,
            @RequestParam(required = false) List<String> filterField,
            @RequestParam(required = false) List<String> filterOperator,
            @RequestParam(required = false) List<String> filterValue,
            @RequestParam(defaultValue = "false") boolean all
    ) {
        return ResponseEntity.ok(orderService.getPagedOrders(
                page, pageSize, sortField, sortOrder, filterField, filterOperator, filterValue, all
        ));
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderDTO dto) {
        try {
            OrderDTO order = orderService.createOrder(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка створення замовлення: " + e.getMessage());
        }
    }

    @PutMapping("/{orderNumber}")
    public ResponseEntity<?> updateOrder(@PathVariable String orderNumber, @RequestBody OrderDTO dto) {
        try {
            orderService.updateOrder(orderNumber, dto);
            return ResponseEntity.ok("Замовлення оновлено");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка оновлення замовлення: " + e.getMessage());
        }
    }

    @DeleteMapping("/{orderNumber}")
    public ResponseEntity<?> deleteOrder(@PathVariable String orderNumber) {
        try{
            orderService.deleteOrder(orderNumber);
            return ResponseEntity.noContent().build();
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка видалення замовлення: " + e.getMessage());
        }
    }
}
