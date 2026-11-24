package com.vanokal0.warehouse_backend.repositories;

import com.vanokal0.warehouse_backend.model.RoomHumidity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomHumidityRepository extends JpaRepository<RoomHumidity, Integer> {
    Optional<RoomHumidity> findByHumidity(Integer humidity);
}
