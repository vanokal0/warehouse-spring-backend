package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.ActionLogDTO;
import com.vanokal0.warehouse_backend.mappers.ActionLogMapper;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActionLogService{

    private final ActionLogRepository actionLogRepository;
    private final ActionLogMapper actionLogMapper;
    private final ParkingLotRepository parkingLotRepository;
    private final RoomRepository roomRepository;
    private final RoomHumidityRepository roomHumidityRepository;
    private final RoomTemperatureRepository roomTemperatureRepository;
    private final ProductTypeRepository productTypeRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final LoadingDockRepository loadingDockRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeePositionRepository employeePositionRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderRepository orderRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final SupplierRepository supplierRepository;

    public List<ActionLogDTO> getAllLogs() {
        return actionLogRepository.findAll().stream().map(actionLogMapper::toDTO).toList();
    }


    public List<ActionLogDTO> getLogsByUserId(Integer userId) {
        return actionLogRepository.findByUserId(userId).stream().map(actionLogMapper::toDTO).toList();
    }


    public ActionLogDTO getLogById(String id) {
        return actionLogMapper.toDTO(actionLogRepository.findById(id).orElseThrow());
    }


    public boolean cancelAction(String id) {
        ActionLog log = actionLogRepository.findById(id).orElseThrow(()->new NoSuchElementException("Action not found"));


        switch (log.getEntityType()){
            case "Room":{
                Room room = roomRepository.findById(log.getEntityId()).orElseThrow(()-> new NoSuchElementException("Room not found."));
                switch(log.getAction()){
                    case "CREATE":{
                        room.setIsActive(false);
                        break;
                    }
                    case "UPDATE":{
                        room.setAvailableSpace((Integer) log.getPreviousState().get("availableSpace"));
                        room.setHumidity(roomHumidityRepository.findByHumidity((Integer) log.getPreviousState().get("humidity")).orElseThrow());
                        room.setTemperature(roomTemperatureRepository.findByTemperature((String) log.getPreviousState().get("temperature")).orElseThrow());
                        room.setProductType(productTypeRepository.findByTypeName((String) log.getPreviousState().get("productType")).orElseThrow());
                        break;
                    }
                    case "DELETE":{
                        room.setIsActive(true);
                        break;
                    }
                }
                roomRepository.save(room);
                break;
            }
            case "ParkingLot":{
                ParkingLot parkingLot = parkingLotRepository.findById(log.getEntityId()).orElseThrow(()-> new NoSuchElementException("Parking lot not found."));
                switch(log.getAction()){
                    case "CREATE":{
                        parkingLot.setIsActive(false);
                        break;
                    }
                    case "UPDATE":{
                        parkingLot.setVehicleType(vehicleTypeRepository.findByTypeName((String) log.getPreviousState().get("vehicleType")).orElseThrow());
                        parkingLot.setLoadingDock(loadingDockRepository.findByDockNumber((String) log.getPreviousState().get("dockNumber")).orElseThrow());
                        break;
                    }
                    case "DELETE":{
                        parkingLot.setIsActive(true);
                        break;
                    }
                }
                parkingLotRepository.save(parkingLot);
                break;
            }
            case "Employee":{
                Employee employee = employeeRepository.findById(log.getEntityId()).orElseThrow(()-> new NoSuchElementException("Employee not found."));
                switch(log.getAction()){
                    case "CREATE":{
                        employee.setIsActive(false);
                        break;
                    }
                    case "UPDATE":{
                        employee.setFirstName((String) log.getPreviousState().get("firstName"));
                        employee.setLastName((String) log.getPreviousState().get("lastName"));
                        employee.setPosition(employeePositionRepository.findByPositionName((String) log.getPreviousState().get("position")).orElseThrow());
                        break;
                    }
                    case "DELETE":{
                        employee.setIsActive(true);
                        break;
                    }
                }
                employeeRepository.save(employee);
                break;
            }
            case "Warehouse":{
                Warehouse warehouse = warehouseRepository.findById(log.getEntityId()).orElseThrow(()->new NoSuchElementException("Warehouse not found."));
                switch(log.getAction()){
                    case "CREATE":{
                        warehouse.setIsActive(false);
                        break;
                    }
                    case "UPDATE":{
                        warehouse.setNumber((String) log.getPreviousState().get("number"));
                        warehouse.setAddress((String) log.getPreviousState().get("address"));
                        break;
                    }
                    case "DELETE":{
                        warehouse.setIsActive(true);
                        break;
                    }
                }
                warehouseRepository.save(warehouse);
            break;
            }
//            case "Order":{
//
//                switch(log.getAction()){
//                    case "CREATE":{
//                        Order order = orderRepository.findById(log.getEntityId()).orElseThrow(()->new NoSuchElementException("Warehouse not found."));
//                        orderRepository.delete(order);
//                        break;
//                    }
//                    case "UPDATE":{
//                        Order order = orderRepository.findById(log.getEntityId()).orElseThrow(()->new NoSuchElementException("Warehouse not found."));
//                        order.setOrderStatus(orderStatusRepository.findByStatusName((String) log.getPreviousState().get("status")).orElseThrow());
//                        order.setPaymentType(paymentTypeRepository.findByTypeName((String) log.getPreviousState().get("paymentType")).orElseThrow());
//                        order.setSupplier(supplierRepository.findByCompanyName((String) log.getPreviousState().get("supplier")).orElseThrow());
//                        order.setSupplier(supplierRepository.findByCompanyName((String) log.getPreviousState().get("supplier")).orElseThrow());
//                        order.setOrderDateTime((Instant) log.getPreviousState().get("orderDateTime"));
//                        order.setEstimatedArrivalDate((LocalDate) log.getPreviousState().get("estimatedArrivalDate"));
//                        order.setEstimatedShipmentDate((LocalDate) log.getPreviousState().get("estimatedShipmentDate"));
//                        order.setOrderCost((BigDecimal) log.getPreviousState().get("cost"));
//                        orderRepository.save(order);
//                        break;
//                    }
//                    case "DELETE":{
//                        Order order = new Order();
//                        order.setOrderStatus(orderStatusRepository.findByStatusName((String) log.getPreviousState().get("status")).orElseThrow());
//                        order.setPaymentType(paymentTypeRepository.findByTypeName((String) log.getPreviousState().get("paymentType")).orElseThrow());
//                        order.setSupplier(supplierRepository.findByCompanyName((String) log.getPreviousState().get("supplier")).orElseThrow());
//                        order.setSupplier(supplierRepository.findByCompanyName((String) log.getPreviousState().get("supplier")).orElseThrow());
//                        order.setOrderDateTime((Instant) log.getPreviousState().get("orderDateTime"));
//                        order.setEstimatedArrivalDate((LocalDate) log.getPreviousState().get("estimatedArrivalDate"));
//                        order.setEstimatedShipmentDate((LocalDate) log.getPreviousState().get("estimatedShipmentDate"));
//                        order.setOrderCost((BigDecimal) log.getPreviousState().get("cost"));
//                        orderRepository.save(order);
//                        break;
//                    }
//                }
//                break;
//            }

        }

        log.setCancelled(true);
        actionLogRepository.save(log);
        return true;
    }

    public void logAction(
            Integer userId,
            String action,
            String entityType,
            Integer entityId,
            Map<String, Object> previousState,
            Map<String, Object> newState
    ) {
        ActionLog log = new ActionLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTimestamp(LocalDateTime.now());
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setPreviousState(previousState);
        log.setNewState(newState);
        log.setCancelled(false);

        actionLogRepository.save(log);
    }
}
