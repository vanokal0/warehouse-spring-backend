package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.ConcreteProductDTO;
import com.vanokal0.warehouse_backend.dto.OrderDTO;
import com.vanokal0.warehouse_backend.model.ConcreteProduct;
import com.vanokal0.warehouse_backend.model.Order;
import com.vanokal0.warehouse_backend.repositories.OrderProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.vanokal0.warehouse_backend.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMapperTest {

    @Mock
    private OrderProductRepository orderProductRepository;
    @Mock
    private ConcreteProductMapper concreteProductMapper;

    @InjectMocks
    private OrderMapper mapper;

    @Test
    void mapsOrderFieldsAndProductLines() {
        Order order = order(7, "ACM-777", "CREATED");
        ConcreteProduct apple = concreteProduct(1, "Apple");
        ConcreteProduct pear = concreteProduct(2, "Pear");
        ConcreteProductDTO appleDto = new ConcreteProductDTO(1, "Apple", null, null, null, null, null);
        ConcreteProductDTO pearDto = new ConcreteProductDTO(2, "Pear", null, null, null, null, null);
        when(orderProductRepository.findOrderProductsByOrderId(7)).thenReturn(List.of(
                orderProduct(1, order, apple, 3),
                orderProduct(2, order, pear, 9)));
        when(concreteProductMapper.toDTO(apple)).thenReturn(appleDto);
        when(concreteProductMapper.toDTO(pear)).thenReturn(pearDto);

        OrderDTO dto = mapper.toDTO(order);

        assertThat(dto.getNumber()).isEqualTo("ACM-777");
        assertThat(dto.getCost()).isEqualByComparingTo("1000.00");
        assertThat(dto.getOrderDateTime()).isEqualTo(order.getOrderDateTime());
        assertThat(dto.getEstimatedArrivalDate()).isEqualTo(order.getEstimatedArrivalDate());
        assertThat(dto.getEstimatedShipmentDate()).isEqualTo(order.getEstimatedShipmentDate());
        assertThat(dto.getPaymentType()).isEqualTo("CARD");
        assertThat(dto.getOrderStatus()).isEqualTo("CREATED");
        assertThat(dto.getSupplier()).isEqualTo("Acme Corp");
        assertThat(dto.getProducts())
                .extracting(p -> p.getConcreteProduct(), p -> p.getQuantity(), p -> p.getOrderNumber())
                .containsExactly(
                        tuple(appleDto, 3, "ACM-777"),
                        tuple(pearDto, 9, "ACM-777"));
    }

    @Test
    void mapsOrderWithoutProducts() {
        Order order = order(8, "ACM-888", "SHIPPED");
        when(orderProductRepository.findOrderProductsByOrderId(8)).thenReturn(List.of());

        OrderDTO dto = mapper.toDTO(order);

        assertThat(dto.getProducts()).isEmpty();
        assertThat(dto.getOrderStatus()).isEqualTo("SHIPPED");
    }
}
