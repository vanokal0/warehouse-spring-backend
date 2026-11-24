package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.RoomDTO;
import com.vanokal0.warehouse_backend.mappers.RoomMapper;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;

@Service
public class RoomService {

    private final Integer MANAGER_ID = 110;
    private final ProductTypeRepository productTypeRepository;
    private final WarehouseRepository warehouseRepository;
    private final RoomRepository roomRepository;
    private final RoomHumidityRepository roomHumidityRepository;
    private final RoomTemperatureRepository roomTemperatureRepository;
    private final RoomMapper roomMapper;
    private final ActionLogService actionLogService;

    @Autowired
    public RoomService(
            ProductTypeRepository productTypeRepository,
            WarehouseRepository warehouseRepository,
            RoomRepository roomRepository,
            RoomHumidityRepository roomHumidityRepository,
            RoomTemperatureRepository roomTemperatureRepository,
            RoomMapper roomMapper,
            ActionLogService actionLogService
    ) {
        this.productTypeRepository = productTypeRepository;
        this.warehouseRepository = warehouseRepository;
        this.roomRepository = roomRepository;
        this.roomHumidityRepository = roomHumidityRepository;
        this.roomTemperatureRepository = roomTemperatureRepository;
        this.roomMapper = roomMapper;
        this.actionLogService = actionLogService;
    }

    public List<String> getAvailableRoomNumbersByWarehouseAndType(String warehouseNumber, String productTypeName) {
        ProductType productType = productTypeRepository.findByTypeName(productTypeName).orElseThrow();
        Warehouse warehouse = warehouseRepository.findByNumber(warehouseNumber).orElseThrow();

        return roomRepository.findRoomByWarehouseAndProductType(warehouse, productType)
                .stream()
                .map(Room::getRoomNumber)
                .toList();
    }

    public RoomDTO createRoom(RoomDTO dto) {
        Warehouse warehouse = warehouseRepository.findByNumber(dto.getWarehouseNumber())
                .orElseThrow(() -> new NoSuchElementException("Склад не знайдено"));

        ProductType productType = findOrCreateProductType(dto.getType());
        RoomTemperature temperature = findOrCreateTemperature(dto.getTemperature());
        RoomHumidity humidity = findOrCreateHumidity(dto.getHumidity());

        Room room = new Room();
        room.setWarehouse(warehouse);
        room.setRoomNumber(warehouse.getNumber() + "-" + (roomRepository.countByWarehouse(warehouse) + 1));
        room.setAvailableSpace(dto.getAvailableSpace());
        room.setProductType(productType);
        room.setTemperature(temperature);
        room.setHumidity(humidity);
        room.setIsActive(true);

        Room saved = roomRepository.save(room);

        actionLogService.logAction(
                MANAGER_ID,
                "CREATE",
                "Room",
                saved.getId(),
                null,
                new HashMap<>()
        );

        return roomMapper.toDTO(saved);
    }

    public RoomDTO updateRoom(String number, RoomDTO dto) {
        Room room = roomRepository.findByRoomNumber(number)
                .orElseThrow(() -> new NoSuchElementException("Кімнату не знайдено"));
        Map<String, Object> oldState = new HashMap<>();
        oldState.put("number", room.getRoomNumber());
        oldState.put("warehouse" , room.getWarehouse().getNumber());
        oldState.put("humidity", room.getHumidity().getHumidity());
        oldState.put("temperature", room.getTemperature().getTemperature());
        oldState.put("availableSpace", room.getAvailableSpace());
        oldState.put("productType", room.getProductType().getTypeName());
        ProductType productType = findOrCreateProductType(dto.getType());
        RoomTemperature temperature = findOrCreateTemperature(dto.getTemperature());
        RoomHumidity humidity = findOrCreateHumidity(dto.getHumidity());

        room.setAvailableSpace(dto.getAvailableSpace());
        room.setProductType(productType);
        room.setTemperature(temperature);
        room.setHumidity(humidity);

        Room updated = roomRepository.save(room);

        Map<String, Object> newState = new HashMap<>();
        oldState.put("number", updated.getRoomNumber());
        oldState.put("warehouse" , updated.getWarehouse().getNumber());
        oldState.put("humidity", updated.getHumidity().getHumidity());
        oldState.put("temperature", updated.getTemperature().getTemperature());
        oldState.put("availableSpace", updated.getAvailableSpace());
        oldState.put("productType", updated.getProductType().getTypeName());

        actionLogService.logAction(
                MANAGER_ID,
                "UPDATE",
                "Room",
                updated.getId(),
                oldState,
                newState
        );

        return roomMapper.toDTO(updated);
    }

    public void archiveRoom(String number) {
        Room room = roomRepository.findByRoomNumber(number)
                .orElseThrow(() -> new NoSuchElementException("Кімнату не знайдено"));

        Map<String, Object> prevState = new HashMap<>();

        room.setIsActive(false);
        roomRepository.save(room);

        actionLogService.logAction(
                MANAGER_ID,
                "DELETE",
                "Room",
                room.getId(),
                prevState,
                null
        );
    }

    private ProductType findOrCreateProductType(String typeName) {
        return productTypeRepository.findByTypeName(typeName)
                .orElseGet(() -> {
                    ProductType roomProductType = new ProductType();
                    roomProductType.setTypeName(typeName);
                    return productTypeRepository.save(roomProductType);
                });
    }

    private RoomTemperature findOrCreateTemperature(String temperatureValue) {
        return roomTemperatureRepository.findByTemperature(temperatureValue)
                .orElseGet(() -> {
                    RoomTemperature roomTemperature = new RoomTemperature();
                    roomTemperature.setTemperature(temperatureValue);
                    return roomTemperatureRepository.save(roomTemperature);
                });
    }

    private RoomHumidity findOrCreateHumidity(Integer humidityValue) {
        return roomHumidityRepository.findByHumidity(humidityValue)
                .orElseGet(() -> {
                    RoomHumidity roomHumidity = new RoomHumidity();
                    roomHumidity.setHumidity(humidityValue);
                    return roomHumidityRepository.save(roomHumidity);
                });
    }
}
