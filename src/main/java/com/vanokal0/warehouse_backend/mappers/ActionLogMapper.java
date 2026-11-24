package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.ActionLogDTO;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import org.springframework.stereotype.Component;

import javax.naming.OperationNotSupportedException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ActionLogMapper implements ModelDTOMapper<ActionLog, ActionLogDTO>{

    private final EmployeeRepository employeeRepository;
    private final RoomRepository roomRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final WarehouseRepository warehouseRepository;
    private final ConcreteProductRepository concreteProductRepository;
    private final OrderRepository orderRepository;
    private final ProductPlacementRepository productPlacementRepository;
    private final ProductShipmentRepository productShipmentRepository;
    private final LotBookingRepository lotBookingRepository;

    public ActionLogMapper(EmployeeRepository employeeRepository,
                           RoomRepository roomRepository,
                           ParkingLotRepository parkingLotRepository,
                           WarehouseRepository warehouseRepository,
                           ConcreteProductRepository concreteProductRepository,
                           OrderRepository orderRepository,
                           ProductPlacementRepository productPlacementRepository,
                           ProductShipmentRepository productShipmentRepository,
                           LotBookingRepository lotBookingRepository) {
        this.employeeRepository = employeeRepository;
        this.roomRepository = roomRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.warehouseRepository = warehouseRepository;
        this.concreteProductRepository = concreteProductRepository;
        this.orderRepository = orderRepository;
        this.productPlacementRepository = productPlacementRepository;
        this.productShipmentRepository = productShipmentRepository;
        this.lotBookingRepository = lotBookingRepository;
    }

    @Override
    public ActionLogDTO toDTO(ActionLog actionLog) {
        Employee initiator = employeeRepository.findById(actionLog.getUserId())
                .orElseThrow();

        String entityName = actionLog.getEntityId().toString();
        try{
            switch(actionLog.getEntityType()){
                case "Warehouse":{
                    Warehouse entity = warehouseRepository.findById(actionLog.getEntityId()).orElseThrow();
                    entityName = entity.getNumber();
                    break;
                }
                case "Employee":{
                    Employee entity = employeeRepository.findById(actionLog.getEntityId()).orElseThrow();
                    entityName = entity.getFirstName()+" "+entity.getLastName();
                    break;
                }
                case "Room":{
                    Room entity = roomRepository.findById(actionLog.getEntityId()).orElseThrow();
                    entityName = entity.getRoomNumber();
                    break;
                }
                case "ParkingLot":{
                    ParkingLot entity = parkingLotRepository.findById(actionLog.getEntityId()).orElseThrow();
                    entityName = entity.getLotNumber();
                    break;
                }
                case "ConcreteProduct":{
                    ConcreteProduct entity = concreteProductRepository.findById(actionLog.getEntityId()).orElseThrow();
                    entityName = entity.getProductName();
                    break;
                }
                case "Order":{
                    Order entity = orderRepository.findById(actionLog.getEntityId()).orElseThrow();
                    entityName = entity.getOrderNumber();
                    break;
                }
                case "ProductPlacement":{
                    ProductPlacement entity = productPlacementRepository.findById(actionLog.getEntityId()).orElseThrow();
                    entityName = entity.getPlacementNumber();
                    break;
                }
                case "ProductShipment":{
                    ProductShipment entity = productShipmentRepository.findById(actionLog.getEntityId()).orElseThrow();
                    entityName = entity.getShipmentNumber();
                    break;
                }
                case "LotBooking":{
                    LotBooking entity = lotBookingRepository.findById(actionLog.getEntityId()).orElseThrow();
                    entityName = entity.getBookingNumber();
                    break;
                }

        }


        }catch (Exception e){
            entityName = "deleted";
        }

        Map<String, String> prev = new HashMap<>();
        Map<String, String> curr = new HashMap<>();

        if (actionLog.getPreviousState() != null) {
            actionLog.getPreviousState().forEach((k, v) ->
                    prev.put(k, v != null ? v.toString() : "null")
            );
        }
        if (actionLog.getNewState() != null) {
            actionLog.getNewState().forEach((k, v) ->
                    curr.put(k, v != null ? v.toString() : "null")
            );
        }


        return new ActionLogDTO(
                actionLog.getId(),
                initiator.getFirstName()+" "+initiator.getLastName(),
                actionLog.getTimestamp(),
                actionLog.isCancelled(),
                actionLog.getAction(),
                entityName,
                actionLog.getEntityType(),
                prev,
                curr
                );
    }
}
