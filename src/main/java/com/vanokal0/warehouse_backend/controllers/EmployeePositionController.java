package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.services.EmployeePositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee-positions")
public class EmployeePositionController {

    private final EmployeePositionService service;

    @Autowired
    public EmployeePositionController(EmployeePositionService service) {
        this.service = service;
    }

    @GetMapping("/position-names")
    public List<String> getAllPositionNames() {
        return service.getAllPositionNames();
    }
}
