package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.RoomTemperature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomTemperatureRepository extends JpaRepository<RoomTemperature, Integer> {
    Optional<RoomTemperature> findByTemperature(String temperature);
}
