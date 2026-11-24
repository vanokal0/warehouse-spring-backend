package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.*;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.EmployeeRepository;
import com.vanokal0.warehouse_backend.repositories.LoadingDockRepository;
import com.vanokal0.warehouse_backend.repositories.ParkingLotRepository;
import com.vanokal0.warehouse_backend.repositories.RoomRepository;
import com.vanokal0.warehouse_backend.utils.GeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WarehouseMapper implements ModelDTOMapper<Warehouse, WarehouseDTO> {
    private final RoomRepository roomRepository;
    private final LoadingDockRepository loadingDockRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final EmployeeRepository employeeRepository;
    private final GeocodingService geocodingService;
    private final EmployeeMapper employeeMapper;
    private final ParkingLotMapper parkingLotMapper;
    private final RoomMapper roomMapper;

    @Autowired
    public WarehouseMapper(RoomRepository roomRepository, LoadingDockRepository loadingDockRepository, ParkingLotRepository parkingLotRepository, EmployeeRepository employeeRepository, GeocodingService geocodingService, EmployeeMapper employeeMapper, ParkingLotMapper parkingLotMapper, RoomMapper roomMapper){
        this.roomRepository = roomRepository;
        this.loadingDockRepository = loadingDockRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.employeeRepository = employeeRepository;
        this.geocodingService = geocodingService;
        this.employeeMapper = employeeMapper;
        this.parkingLotMapper = parkingLotMapper;
        this.roomMapper = roomMapper;
    }

    @Override
    public WarehouseDTO toDTO(Warehouse warehouse) {
        List<RoomDTO> rooms = roomRepository.findByWarehouseId(warehouse.getId()).stream().filter(Room::getIsActive)
                .map(roomMapper::toDTO)
                .toList();

        List<LoadingDockDTO> docks = loadingDockRepository.findByWarehouseId(warehouse.getId()).stream().filter(LoadingDock::getIsActive)
                .map(dock -> new LoadingDockDTO(dock.getDockNumber(), parkingLotRepository.findByLoadingDockId(dock.getId()).stream().filter(ParkingLot::getIsActive).map(parkingLotMapper::toDTO).toList()))
                .toList();

        List<EmployeeDTO> employees = employeeRepository.findByWarehouseId(warehouse.getId()).stream()
                .filter(Employee::getIsActive).map(employeeMapper::toDTO)
                .toList();

        return new WarehouseDTO(
                warehouse.getId(),
                warehouse.getNumber(),
                warehouse.getAddress(),
                geocodingService.getGeocodingAPIResponse(warehouse.getAddress()),
                rooms,
                employees,
                docks
        );
    }
}
