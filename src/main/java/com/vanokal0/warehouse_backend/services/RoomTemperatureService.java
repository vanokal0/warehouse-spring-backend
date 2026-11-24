package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.model.RoomTemperature;
import com.vanokal0.warehouse_backend.repositories.RoomTemperatureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomTemperatureService {

    private final RoomTemperatureRepository repository;

    @Autowired
    public RoomTemperatureService(RoomTemperatureRepository repository) {
        this.repository = repository;
    }

    public List<String> getAllTemperatures() {
        return repository.findAll()
                .stream()
                .map(RoomTemperature::getTemperature)
                .toList();
    }
}
