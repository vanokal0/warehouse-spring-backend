package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.EmployeeDTO;
import com.vanokal0.warehouse_backend.mappers.EmployeeMapper;
import com.vanokal0.warehouse_backend.model.Employee;
import com.vanokal0.warehouse_backend.model.EmployeePosition;
import com.vanokal0.warehouse_backend.model.Warehouse;
import com.vanokal0.warehouse_backend.repositories.EmployeePositionRepository;
import com.vanokal0.warehouse_backend.repositories.EmployeeRepository;
import com.vanokal0.warehouse_backend.repositories.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EmployeeService {
    private final Integer MANAGER_ID = 110;
    private final WarehouseRepository warehouseRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeePositionRepository employeePositionRepository;
    private final EmployeeMapper employeeMapper;
    private final ActionLogService actionLogService;

    @Autowired
    public EmployeeService(
            WarehouseRepository warehouseRepository,
            EmployeeRepository employeeRepository,
            EmployeePositionRepository employeePositionRepository,
            EmployeeMapper employeeMapper,
            ActionLogService actionLogService
    ) {
        this.warehouseRepository = warehouseRepository;
        this.employeeRepository = employeeRepository;
        this.employeePositionRepository = employeePositionRepository;
        this.employeeMapper = employeeMapper;
        this.actionLogService = actionLogService;
    }

    public List<String> getOperatorFullNamesByWarehouse(String number) {
        Warehouse warehouse = warehouseRepository.findByNumber(number).orElseThrow();
        EmployeePosition position = employeePositionRepository.findByPositionName("Operator").orElseThrow();

        return employeeRepository.findAllByWarehouseAndPosition(
                        warehouse, position
                ).stream().filter(Employee::getIsActive)
                .map(e -> e.getFirstName() + " " + e.getLastName())
                .toList();
    }

    public EmployeeDTO createEmployee(EmployeeDTO dto ) {
        EmployeePosition position = employeePositionRepository
                .findByPositionName(dto.getPosition())
                .orElseGet(() -> {
                    EmployeePosition newPosition = new EmployeePosition();
                    newPosition.setPositionName(dto.getPosition());
                    return employeePositionRepository.save(newPosition);
                });

        Warehouse warehouse = warehouseRepository.findByNumber(dto.getWarehouseNumber()).orElseThrow();

        Employee employee = new Employee();
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setPosition(position);
        employee.setWarehouse(warehouse);
        employee.setIsActive(true);

        Employee saved = employeeRepository.save(employee);

        actionLogService.logAction(
                MANAGER_ID,
                "CREATE",
                "Employee",
                saved.getId(),
                null,
                Map.of(
                        "firstName", saved.getFirstName(),
                        "lastName", saved.getLastName(),
                        "position", saved.getPosition().getPositionName(),
                        "warehouse", saved.getWarehouse().getNumber(),
                        "isActive", true
                )
        );

        return employeeMapper.toDTO(saved);
    }

    public EmployeeDTO updateEmployee(Integer id, EmployeeDTO dto) {
        Employee employee = employeeRepository
                .findById(id)
                .orElseThrow(() -> new NoSuchElementException("Працівника не знайдено"));

        Map<String, Object> prevState = Map.of(
                "firstName", employee.getFirstName(),
                "lastName", employee.getLastName(),
                "position", employee.getPosition().getPositionName()
        );

        EmployeePosition position = employeePositionRepository
                .findByPositionName(dto.getPosition())
                .orElseGet(() -> {
                    EmployeePosition newPosition = new EmployeePosition();
                    newPosition.setPositionName(dto.getPosition());
                    return employeePositionRepository.save(newPosition);
                });

        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setPosition(position);

        Employee saved = employeeRepository.save(employee);

        actionLogService.logAction(
                MANAGER_ID,
                "UPDATE",
                "Employee",
                id,
                prevState,
                Map.of(
                        "firstName", saved.getFirstName(),
                        "lastName", saved.getLastName(),
                        "position", saved.getPosition().getPositionName()
                )
        );

        return employeeMapper.toDTO(saved);
    }

    public void archiveEmployee(Integer id) {
        Employee employee = employeeRepository
                .findById(id)
                .orElseThrow(() -> new NoSuchElementException("Працівника не знайдено"));

        Map<String, Object> prevState = Map.of("isActive", employee.getIsActive());

        employee.setIsActive(false);
        employeeRepository.save(employee);

        actionLogService.logAction(
                MANAGER_ID,
                "ARCHIVE",
                "Employee",
                id,
                prevState,
                Map.of("isActive", false)
        );
    }
}
