package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.LotBookingDTO;
import com.vanokal0.warehouse_backend.services.LotBookingService;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/lot-bookings")
public class LotBookingController {

    private final LotBookingService lotBookingService;

    @Autowired
    public LotBookingController(LotBookingService lotBookingService) {
        this.lotBookingService = lotBookingService;
    }

    @GetMapping("/paged")
    public ResponseEntity<Map<String, Object>> getPagedLotBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) List<String> sortField,
            @RequestParam(required = false) List<String> sortOrder,
            @RequestParam(required = false) List<String> filterField,
            @RequestParam(required = false) List<String> filterOperator,
            @RequestParam(required = false) List<String> filterValue,
            @RequestParam(defaultValue = "false") boolean all
    ) {
        return ResponseEntity.ok(
                lotBookingService.getPagedLotBookings(page, pageSize, sortField, sortOrder, filterField, filterOperator, filterValue, all)
        );
    }

    @GetMapping("/by-lot/{lotNumber}")
    public ResponseEntity<Map<String, Object>> getBookingsByLot(
            @PathVariable String lotNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) List<String> sortField,
            @RequestParam(required = false) List<String> sortOrder,
            @RequestParam(required = false) List<String> filterField,
            @RequestParam(required = false) List<String> filterOperator,
            @RequestParam(required = false) List<String> filterValue,
            @RequestParam(defaultValue = "false") boolean all
    ) {
        return ResponseEntity.ok(
                lotBookingService.getPagedLotBookingsForLot(
                        page, pageSize, sortField, sortOrder, filterField, filterOperator, filterValue, lotNumber, all
                )
        );
    }

    @GetMapping("/warehouse")
    public ResponseEntity<List<String>> getBookingNumbersByWarehouse(@RequestParam String warehouseNumber) {
        return ResponseEntity.ok(lotBookingService.getBookingNumbersByWarehouseNumber(warehouseNumber));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody LotBookingDTO dto) {

        try {
            return ResponseEntity.ok(lotBookingService.createLotBooking(dto));
        }
        catch (Exception exception){
            return ResponseEntity.status(HttpStatus.SC_NOT_ACCEPTABLE)
                    .body("Помилка створення бронювання: "+ exception.getMessage());
        }
    }

    @PutMapping("/{number}")
    public ResponseEntity<?> update(@PathVariable String number,
                                                @RequestBody LotBookingDTO dto) {
        try {
            return ResponseEntity.ok(lotBookingService.updateLotBooking(number, dto));
        }
        catch (Exception exception){
            return ResponseEntity.status(HttpStatus.SC_NOT_ACCEPTABLE)
                    .body("Помилка оновлення бронювання: "+ exception.getMessage());
        }

    }

    @DeleteMapping("/{number}")
    public ResponseEntity<String> delete(@PathVariable String number) {
        try {
            lotBookingService.deleteLotBooking(number);
            return ResponseEntity.ok("Бронювання "+number+" видалено успішно");
        }
        catch (Exception exception){
            return ResponseEntity.status(HttpStatus.SC_NOT_ACCEPTABLE)
                    .body("Помилка видалення бронювання: "+ exception.getMessage());
        }


    }
}

