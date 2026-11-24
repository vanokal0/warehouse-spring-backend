package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.services.RoomTemperatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-temperatures")
public class RoomTemperatureController {

    private final RoomTemperatureService service;

    @Autowired
    public RoomTemperatureController(RoomTemperatureService service) {
        this.service = service;
    }

    @GetMapping("/values")
    public List<String> getAllTemperatures() {
        return service.getAllTemperatures();
    }
}
