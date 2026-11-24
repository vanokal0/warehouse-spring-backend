package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.*;
import com.vanokal0.warehouse_backend.dto.geocoding.GeocodeLocation;
import com.vanokal0.warehouse_backend.dto.lightweight.WarehouseCreateUpdateDTO;
import com.vanokal0.warehouse_backend.mappers.WarehouseMapper;
import com.vanokal0.warehouse_backend.model.Warehouse;
import com.vanokal0.warehouse_backend.repositories.*;
import com.vanokal0.warehouse_backend.utils.GeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;


@Service
public class WarehouseService {
    private final Integer MANAGER_ID = 110;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;
    private final ActionLogService actionLogService;

    @Autowired
    public WarehouseService(WarehouseRepository warehouseRepository, EmployeeRepository employeeRepository, RoomRepository roomRepository, LoadingDockRepository loadingDockRepository, ParkingLotRepository parkingLotRepository, GeocodingService geocodingService, WarehouseMapper warehouseMapper, ActionLogService actionLogService
                            ) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseMapper = warehouseMapper;
        this.actionLogService = actionLogService;
    }

    public List<WarehouseDTO> getAllWarehouses() {
        return warehouseRepository.findAll().stream().filter(Warehouse::getIsActive)
                .map(warehouseMapper::toDTO)
                .toList();
    }

    public WarehouseDTO getWarehouseByNumber(String number) {
        Warehouse warehouse = warehouseRepository.findByNumber(number).filter(Warehouse::getIsActive)
                .orElseThrow(() -> new NoSuchElementException("Warehouse with number " + number + " not found"));

        return warehouseMapper.toDTO(warehouse);
    }

    public WarehouseDTO createWarehouse(WarehouseCreateUpdateDTO dto) {
        Warehouse warehouse = new Warehouse();
        if(warehouseRepository.existsByNumber(dto.getNumber())){
            throw new NoSuchElementException("Склад з таким номером вже існує.");
        }
        warehouse.setNumber(dto.getNumber());
        warehouse.setAddress(dto.getAddress());
        warehouse.setIsActive(true);
        Warehouse newWarehouse = warehouseRepository.save(warehouse);
        actionLogService.logAction(MANAGER_ID, "CREATE", "Warehouse", newWarehouse.getId(), null, null);

        return warehouseMapper.toDTO(newWarehouse);
    }

    public WarehouseDTO updateWarehouse(Integer id, WarehouseCreateUpdateDTO dto) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Склад не знайдено"));

        HashMap<String, Object> prevState = new HashMap<>();

        if(!warehouse.getNumber().equals(dto.getNumber()) && warehouseRepository.existsByNumber(dto.getNumber())){
            throw new NoSuchElementException("Склад з таким номером вже існує.");
        }
        prevState.put("number", warehouse.getNumber());
        prevState.put("address", warehouse.getAddress());

        warehouse.setNumber(dto.getNumber());
        warehouse.setAddress(dto.getAddress());

        HashMap<String, Object> newState = new HashMap<>();
        newState.put("number", warehouse.getNumber());
        newState.put("address", warehouse.getAddress());
        actionLogService.logAction(MANAGER_ID, "UPDATE", "Warehouse", warehouse.getId(), prevState, newState);

        return warehouseMapper.toDTO(warehouseRepository.save(warehouse));
    }

    public void archiveWarehouse(Integer id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Склад не знайдено"));

        HashMap<String, Object> prevState = new HashMap<>();
        prevState.put("number", warehouse.getNumber());
        prevState.put("address", warehouse.getAddress());
        actionLogService.logAction(MANAGER_ID, "DELETE", "Warehouse", warehouse.getId(), prevState, null);
        warehouse.setIsActive(false);
        warehouseRepository.save(warehouse);
    }


}
