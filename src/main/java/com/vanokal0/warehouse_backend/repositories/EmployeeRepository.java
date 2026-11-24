package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.Employee;
import com.vanokal0.warehouse_backend.model.EmployeePosition;
import com.vanokal0.warehouse_backend.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    List<Employee> findByWarehouseId(Integer warehouseId);
    List<Employee> findByWarehouseIdAndPositionId(Integer warehouseId, Integer positionId);
    List<Employee> findAllByWarehouseAndPosition(Warehouse warehouse, EmployeePosition position);
    Optional<Employee> findByFirstNameAndLastName(String firstName, String lastName);
}
