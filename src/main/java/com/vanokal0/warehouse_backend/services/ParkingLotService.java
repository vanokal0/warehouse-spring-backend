package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.ParkingLotDTO;
import com.vanokal0.warehouse_backend.mappers.ParkingLotMapper;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ParkingLotService {

    private final Integer MANAGER_ID = 110;
    private final WarehouseRepository warehouseRepository;
    private final ParkingLotMapper parkingLotMapper;
    private final ParkingLotRepository parkingLotRepository;
    private final LoadingDockRepository loadingDockRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ActionLogService actionLogService;

    @Autowired
    public ParkingLotService(WarehouseRepository warehouseRepository,
                             ParkingLotMapper parkingLotMapper,
                             ParkingLotRepository parkingLotRepository,
                             LoadingDockRepository loadingDockRepository,
                             VehicleTypeRepository vehicleTypeRepository,
                             ActionLogService actionLogService) {
        this.warehouseRepository = warehouseRepository;
        this.parkingLotMapper = parkingLotMapper;
        this.parkingLotRepository = parkingLotRepository;
        this.loadingDockRepository = loadingDockRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.actionLogService = actionLogService;
    }

    public ParkingLotDTO createParkingLot(ParkingLotDTO dto) {
        Warehouse warehouse = warehouseRepository.findByNumber(dto.getWarehouseNumber()).orElseThrow();
        LoadingDock dock = loadingDockRepository.findByDockNumber(dto.getDockNumber())
                .orElseGet(() -> {
                    LoadingDock newDock = new LoadingDock();
                    newDock.setDockNumber(dto.getDockNumber());
                    newDock.setWarehouse(warehouse);
                    newDock.setLotQuantity(1);
                    newDock.setIsActive(true);
                    return loadingDockRepository.save(newDock);
                });

        VehicleType vehicleType = findOrCreateVehicleType(dto.getType());

        long index = parkingLotRepository.countByLoadingDock(dock) + 1;
        String firstLetter = dto.getType().substring(0, 1).toUpperCase();
        String lotNumber = dock.getDockNumber() + "-" + index + firstLetter;

        ParkingLot lot = new ParkingLot();
        lot.setLoadingDock(dock);
        lot.setVehicleType(vehicleType);
        lot.setLotNumber(lotNumber);
        lot.setIsActive(true);

        ParkingLot saved = parkingLotRepository.save(lot);


        actionLogService.logAction(MANAGER_ID, "CREATE", "ParkingLot", saved.getId(), null, null);

        return parkingLotMapper.toDTO(saved);
    }

    public ParkingLotDTO updateParkingLot(ParkingLotDTO dto) {
        ParkingLot lot = parkingLotRepository.findByLotNumber(dto.getNumber())
                .orElseThrow(() -> new NoSuchElementException("Паркувальний лот не знайдено"));

        Map<String, Object> oldState = new HashMap<>();
        oldState.put("number", lot.getLotNumber());
        oldState.put("type", lot.getVehicleType());

        LoadingDock dock = loadingDockRepository.findByDockNumber(dto.getDockNumber())
                .orElseGet(() -> {
                    LoadingDock newDock = new LoadingDock();
                    newDock.setDockNumber(dto.getDockNumber());
                    newDock.setLotQuantity(0);
                    newDock.setIsActive(true);
                    newDock.setWarehouse(
                            warehouseRepository.findByNumber(dto.getWarehouseNumber())
                                    .orElseThrow(() -> new NoSuchElementException("Невідомий склад"))
                    );
                    return loadingDockRepository.save(newDock);
                });

        VehicleType type = findOrCreateVehicleType(dto.getType());

        lot.setLoadingDock(dock);
        lot.setVehicleType(type);

        ParkingLot saved = parkingLotRepository.save(lot);

        Map<String, Object> newState = new HashMap<>();
        newState.put("dockNumber", saved.getLoadingDock().getDockNumber());
        newState.put("vehicleType", saved.getVehicleType().getTypeName());
        actionLogService.logAction(MANAGER_ID, "UPDATE", "ParkingLot", saved.getId(), oldState, newState);

        return parkingLotMapper.toDTO(saved);
    }

    public void archiveParkingLot(String lotNumber) {
        ParkingLot lot = parkingLotRepository.findByLotNumber(lotNumber)
                .orElseThrow(() -> new NoSuchElementException("Паркувальний лот не знайдено"));



        lot.setIsActive(false);
        ParkingLot saved = parkingLotRepository.save(lot);


        actionLogService.logAction(MANAGER_ID, "DELETE", "ParkingLot", saved.getId(), null, null);
    }

    private VehicleType findOrCreateVehicleType(String typeName) {
        return vehicleTypeRepository.findByTypeName(typeName)
                .orElseGet(() -> {
                    VehicleType newType = new VehicleType();
                    newType.setTypeName(typeName);
                    return vehicleTypeRepository.save(newType);
                });
    }
}
