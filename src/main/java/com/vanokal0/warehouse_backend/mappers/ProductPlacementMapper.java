package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.OrderProductDTO;
import com.vanokal0.warehouse_backend.dto.ProductPlacementDTO;
import com.vanokal0.warehouse_backend.model.ProductPlacement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductPlacementMapper implements ModelDTOMapper<ProductPlacement, ProductPlacementDTO>{

    private final ConcreteProductMapper concreteProductMapper;
    private final EmployeeMapper employeeMapper;

    @Autowired
    public ProductPlacementMapper(ConcreteProductMapper concreteProductMapper, EmployeeMapper employeeMapper) {
        this.concreteProductMapper = concreteProductMapper;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public ProductPlacementDTO toDTO(ProductPlacement productPlacement) {
        return new ProductPlacementDTO(
                productPlacement.getPlacementNumber(),
                productPlacement.getOrderProduct().getOrder().getOrderNumber(),
                productPlacement.getOrderProduct().getConcreteProduct().getProductName(),
                productPlacement.getProductQuantity(),
                productPlacement.getPlacementDateTime(),
                productPlacement.getStoredProduct().getRoom().getRoomNumber(),
                productPlacement.getLotBooking().getParkingLot().getLotNumber(),
                productPlacement.getLotBooking().getBookingNumber(),
                productPlacement.getEmployee().getFirstName()+" "+productPlacement.getEmployee().getLastName());
    }
}
