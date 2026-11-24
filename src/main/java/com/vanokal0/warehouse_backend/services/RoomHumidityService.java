package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.model.RoomHumidity;
import com.vanokal0.warehouse_backend.repositories.RoomHumidityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomHumidityService {

    private final RoomHumidityRepository repository;

    @Autowired
    public RoomHumidityService(RoomHumidityRepository repository) {
        this.repository = repository;
    }

    public List<Integer> getAllHumidities() {
        return repository.findAll()
                .stream()
                .map(h -> h.getHumidity())
                .toList();
    }
}
