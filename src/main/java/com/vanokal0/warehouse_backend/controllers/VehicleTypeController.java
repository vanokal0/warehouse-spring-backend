package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.services.VehicleTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle-types")
public class VehicleTypeController {

    private final VehicleTypeService service;

    @Autowired
    public VehicleTypeController(VehicleTypeService service) {
        this.service = service;
    }

    @GetMapping("/type-names")
    public List<String> getAllTypeNames() {
        return service.getAllTypeNames();
    }
}
