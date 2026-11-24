package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.EmployeePosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeePositionRepository extends JpaRepository<EmployeePosition, Integer> {
    Optional<EmployeePosition> findByPositionName(String positionName);
}
