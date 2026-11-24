package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.model.EmployeePosition;
import com.vanokal0.warehouse_backend.repositories.EmployeePositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeePositionService {
    private final EmployeePositionRepository repository;

    @Autowired
    public EmployeePositionService(EmployeePositionRepository repository) {
        this.repository = repository;
    }

    public List<String> getAllPositionNames() {
        return repository.findAll()
                .stream()
                .map(EmployeePosition::getPositionName)
                .toList();
    }
}
