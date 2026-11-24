package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.WarehouseDTO;
import com.vanokal0.warehouse_backend.dto.lightweight.WarehouseCreateUpdateDTO;
import com.vanokal0.warehouse_backend.services.EmployeeService;
import com.vanokal0.warehouse_backend.services.RoomService;
import com.vanokal0.warehouse_backend.services.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {
    private final RoomService roomService;
    private final EmployeeService employeeService;
    private final WarehouseService warehouseService;
    @Autowired
    public WarehouseController(RoomService roomService, EmployeeService employeeService, WarehouseService warehouseService){
        this.roomService = roomService;
        this.employeeService = employeeService;
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public ResponseEntity<List<WarehouseDTO>> getAllWarehouses(){
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }
    @GetMapping("/{number}")
    public ResponseEntity<WarehouseDTO> getWarehouseByNumber(@PathVariable(value="number") String number){
        return ResponseEntity.ok(warehouseService.getWarehouseByNumber(number));
    }

    @GetMapping("/operators")
    public ResponseEntity<List<String>> getOperators(@RequestParam String warehouseNumber) {
        return ResponseEntity.ok(
                employeeService.getOperatorFullNamesByWarehouse(warehouseNumber)
        );
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<String>> getRoomsForProductType(
            @RequestParam String warehouseNumber,
            @RequestParam String productType
    ) {
        return ResponseEntity.ok(
                roomService.getAvailableRoomNumbersByWarehouseAndType(warehouseNumber, productType)
        );
    }

    @PostMapping
    public ResponseEntity<?> createWarehouse(@RequestBody WarehouseCreateUpdateDTO dto) {
        try{
            return ResponseEntity.ok(warehouseService.createWarehouse(dto));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка створення складу: " + e.getMessage());
        }

    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWarehouse(
            @PathVariable Integer id,
            @RequestBody WarehouseCreateUpdateDTO dto
    ) {

        try{
            return ResponseEntity.ok(warehouseService.updateWarehouse(id, dto));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка оновлення складу: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWarehouse(@PathVariable Integer id) {
        try{
            warehouseService.archiveWarehouse(id);
            return ResponseEntity.noContent().build();
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Помилка видалення складу: " + e.getMessage());
        }

    }

}
