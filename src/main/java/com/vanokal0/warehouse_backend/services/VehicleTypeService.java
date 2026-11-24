package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.model.VehicleType;
import com.vanokal0.warehouse_backend.repositories.VehicleTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleTypeService {

    private final VehicleTypeRepository repository;

    @Autowired
    public VehicleTypeService(VehicleTypeRepository repository) {
        this.repository = repository;
    }

    public List<String> getAllTypeNames() {
        return repository.findAll()
                .stream()
                .map(VehicleType::getTypeName)
                .toList();
    }
}
