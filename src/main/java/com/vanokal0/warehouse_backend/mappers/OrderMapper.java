package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.OrderDTO;
import com.vanokal0.warehouse_backend.dto.OrderProductDTO;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper implements ModelDTOMapper<Order, OrderDTO> {
    private final OrderProductRepository orderProductRepository;
    private final ConcreteProductMapper concreteProductMapper;

    @Autowired
    public OrderMapper(OrderProductRepository orderProductRepository, ConcreteProductMapper concreteProductMapper) {
        this.orderProductRepository = orderProductRepository;
        this.concreteProductMapper = concreteProductMapper;
    }


    @Override
    public OrderDTO toDTO(Order order) {

        List<OrderProductDTO> orderProducts = orderProductRepository.findOrderProductsByOrderId(order.getId()).stream().map(
                orderProduct -> new OrderProductDTO(
                        concreteProductMapper.toDTO(orderProduct.getConcreteProduct()),
                        orderProduct.getProductQuantity(),
                       order.getOrderNumber()
                )
        ).toList();
        return new OrderDTO(order.getOrderNumber(),
                orderProducts, order.getOrderCost(),
                order.getOrderDateTime(),
                order.getEstimatedArrivalDate(),
                order.getEstimatedShipmentDate(),
                order.getPaymentType().getTypeName(),
                order.getOrderStatus().getStatusName(),
                order.getSupplier().getCompanyName()
        );
    }
}
