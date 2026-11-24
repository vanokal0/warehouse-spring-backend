package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.ParkingLotDTO;
import com.vanokal0.warehouse_backend.services.ParkingLotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking-lots")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    @Autowired
    public ParkingLotController(ParkingLotService parkingLotService) {
        this.parkingLotService = parkingLotService;
    }

    @PostMapping
    public ResponseEntity<ParkingLotDTO> create(@RequestBody ParkingLotDTO dto) {
        return ResponseEntity.ok(parkingLotService.createParkingLot(dto));
    }

    @PutMapping("/{number}")
    public ResponseEntity<ParkingLotDTO> update(@RequestBody ParkingLotDTO dto) {
        return ResponseEntity.ok(parkingLotService.updateParkingLot(dto));
    }

    @DeleteMapping("/{lotNumber}")
    public ResponseEntity<Void> delete(@PathVariable String lotNumber) {
        parkingLotService.archiveParkingLot(lotNumber);
        return ResponseEntity.noContent().build();
    }
}
