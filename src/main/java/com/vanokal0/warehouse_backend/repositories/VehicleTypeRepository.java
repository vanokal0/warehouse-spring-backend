package com.vanokal0.warehouse_backend.repositories;

import com.vanokal0.warehouse_backend.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Integer> {
    boolean existsByTypeName(String typeName);
    Optional<VehicleType> findByTypeName(String typeName);
}
