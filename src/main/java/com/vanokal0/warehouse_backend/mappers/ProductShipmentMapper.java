package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.ProductPlacementDTO;
import com.vanokal0.warehouse_backend.dto.ProductShipmentDTO;
import com.vanokal0.warehouse_backend.model.ProductPlacement;
import com.vanokal0.warehouse_backend.model.ProductShipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductShipmentMapper implements ModelDTOMapper<ProductShipment, ProductShipmentDTO>{

    private final ConcreteProductMapper concreteProductMapper;
    private final EmployeeMapper employeeMapper;

    @Autowired
    public ProductShipmentMapper(ConcreteProductMapper concreteProductMapper, EmployeeMapper employeeMapper) {
        this.concreteProductMapper = concreteProductMapper;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public ProductShipmentDTO toDTO(ProductShipment productShipment) {
        return new ProductShipmentDTO(
                productShipment.getShipmentNumber(),
                productShipment.getOrderProduct().getOrder().getOrderNumber(),
                productShipment.getOrderProduct().getConcreteProduct().getProductName(),
                productShipment.getProductQuantity(),
                productShipment.getShipmentDateTime(),
                productShipment.getStoredProduct().getRoom().getRoomNumber(),
                productShipment.getLotBooking().getParkingLot().getLotNumber(),
                productShipment.getLotBooking().getBookingNumber(),
                productShipment.getEmployee().getFirstName()+" "+productShipment.getEmployee().getLastName()
        );
    }
}
