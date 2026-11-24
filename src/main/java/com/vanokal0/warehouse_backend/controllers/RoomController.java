package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.RoomDTO;
import com.vanokal0.warehouse_backend.services.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomDTO> createRoom(@RequestBody RoomDTO dto) {
        return ResponseEntity.ok(roomService.createRoom(dto));
    }

    @PutMapping("/{number}")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable String number, @RequestBody RoomDTO dto) {
        return ResponseEntity.ok(roomService.updateRoom(number, dto));
    }

    @DeleteMapping("/{number}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String number) {
        roomService.archiveRoom(number);
        return ResponseEntity.noContent().build();
    }
}
