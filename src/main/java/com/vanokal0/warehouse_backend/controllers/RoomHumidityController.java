package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.services.RoomHumidityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-humidities")
public class RoomHumidityController {

    private final RoomHumidityService service;

    @Autowired
    public RoomHumidityController(RoomHumidityService service) {
        this.service = service;
    }

    @GetMapping("/values")
    public List<Integer> getAllHumidities() {
        return service.getAllHumidities();
    }
}
