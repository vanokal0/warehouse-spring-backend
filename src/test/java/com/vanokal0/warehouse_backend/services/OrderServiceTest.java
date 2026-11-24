package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.ConcreteProductDTO;
import com.vanokal0.warehouse_backend.dto.OrderDTO;
import com.vanokal0.warehouse_backend.dto.OrderProductDTO;
import com.vanokal0.warehouse_backend.mappers.OrderMapper;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import javax.naming.OperationNotSupportedException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static com.vanokal0.warehouse_backend.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final int MANAGER_ID = 110;

    @Mock
    private ActionLogService actionLogService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private OrderStatusRepository orderStatusRepository;
    @Mock
    private PaymentTypeRepository paymentTypeRepository;
    @Mock
    private ConcreteProductRepository concreteProductRepository;
    @Mock
    private OrderProductRepository orderProductRepository;
    @Mock
    private ProductPlacementRepository productPlacementRepository;
    @Mock
    private ProductShipmentRepository productShipmentRepository;

    @InjectMocks
    private OrderService service;

    @Captor
    private ArgumentCaptor<Map<String, Object>> prevStateCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Object>> newStateCaptor;

    private static OrderProductDTO productDto(String name, int qty) {
        ConcreteProductDTO cp = new ConcreteProductDTO();
        cp.setConcreteProductName(name);
        return new OrderProductDTO(cp, qty, null);
    }

    private static OrderDTO orderDto(String supplier, String status, String payment, OrderProductDTO... products) {
        return new OrderDTO(
                null,
                List.of(products),
                new BigDecimal("2500.00"),
                Instant.parse("2025-04-01T08:00:00Z"),
                LocalDate.of(2025, 4, 20),
                LocalDate.of(2025, 4, 10),
                payment,
                status,
                supplier
        );
    }

    private void stubOrderSaveAssigningId(int id) {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(id);
            return o;
        });
    }

    @Test
    void fieldMapping_translatesDtoFieldsToEntityPaths() {
        assertThat(service.getFieldMapping())
                .containsEntry("number", "orderNumber")
                .containsEntry("cost", "orderCost")
                .containsEntry("paymentType", "paymentType.typeName")
                .containsEntry("orderStatus", "orderStatus.statusName")
                .containsEntry("supplier", "supplier.companyName")
                .containsEntry("orderDateTime", "orderDateTime")
                .containsEntry("estimatedArrivalDate", "estimatedArrivalDate")
                .containsEntry("estimatedShipmentDate", "estimatedShipmentDate");
    }

    @Nested
    class GetPagedOrders {

        @Test
        void returnsMappedItemsAndTotal_usingPageRequestWithSort() {
            Order o1 = order(1, "ACM-100", "CREATED");
            Order o2 = order(2, "ACM-101", "CREATED");
            OrderDTO d1 = new OrderDTO();
            OrderDTO d2 = new OrderDTO();
            when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(o1, o2), PageRequest.of(1, 2), 12));
            when(orderMapper.toDTO(o1)).thenReturn(d1);
            when(orderMapper.toDTO(o2)).thenReturn(d2);

            Map<String, Object> result = service.getPagedOrders(1, 2,
                    List.of("cost"), List.of("desc"), null, null, null, false);

            assertThat(result).containsEntry("total", 12L);
            assertThat((List<Object>) result.get("items")).containsExactly(d1, d2);

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(orderRepository).findAll(any(Specification.class), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(2);
            assertThat(pageable.getValue().getSort().getOrderFor("orderCost").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        void usesUnpagedRequest_whenAllIsTrue() {
            when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Map<String, Object> result = service.getPagedOrders(3, 10, null, null, null, null, null, true);

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(orderRepository).findAll(any(Specification.class), pageable.capture());
            assertThat(pageable.getValue().isUnpaged()).isTrue();
            assertThat(result).containsEntry("total", 0L);
            assertThat((List<?>) result.get("items")).isEmpty();
            verifyNoInteractions(orderMapper);
        }
    }

    @Nested
    class CreateOrder {

        @Test
        void savesOrderWithProducts_logsAction_andReturnsMappedDto() {
            Supplier supplier = supplier("Acme Corp");
            OrderStatus status = orderStatus("CREATED");
            PaymentType payment = paymentType("CARD");
            ConcreteProduct p1 = concreteProduct(1, "Apple");
            ConcreteProduct p2 = concreteProduct(2, "Pear");
            OrderDTO expected = new OrderDTO();

            when(supplierRepository.findByCompanyName("Acme Corp")).thenReturn(Optional.of(supplier));
            when(orderStatusRepository.findByStatusName("CREATED")).thenReturn(Optional.of(status));
            when(paymentTypeRepository.findByTypeName("CARD")).thenReturn(Optional.of(payment));
            when(concreteProductRepository.findByProductName("Apple")).thenReturn(Optional.of(p1));
            when(concreteProductRepository.findByProductName("Pear")).thenReturn(Optional.of(p2));
            stubOrderSaveAssigningId(50);
            when(orderMapper.toDTO(any(Order.class))).thenReturn(expected);

            OrderDTO dto = orderDto("Acme Corp", "CREATED", "CARD", productDto("Apple", 3), productDto("Pear", 5));
            OrderDTO result = service.createOrder(dto);

            assertThat(result).isSameAs(expected);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order saved = orderCaptor.getValue();
            assertThat(saved.getOrderNumber()).matches("ACM-\\d{3}");
            assertThat(saved.getOrderCost()).isEqualByComparingTo("2500.00");
            assertThat(saved.getOrderDateTime()).isEqualTo(dto.getOrderDateTime());
            assertThat(saved.getEstimatedArrivalDate()).isEqualTo(dto.getEstimatedArrivalDate());
            assertThat(saved.getEstimatedShipmentDate()).isEqualTo(dto.getEstimatedShipmentDate());
            assertThat(saved.getSupplier()).isSameAs(supplier);
            assertThat(saved.getOrderStatus()).isSameAs(status);
            assertThat(saved.getPaymentType()).isSameAs(payment);

            ArgumentCaptor<OrderProduct> opCaptor = ArgumentCaptor.forClass(OrderProduct.class);
            verify(orderProductRepository, times(2)).save(opCaptor.capture());
            assertThat(opCaptor.getAllValues())
                    .extracting(OrderProduct::getConcreteProduct, OrderProduct::getProductQuantity, OrderProduct::getOrder)
                    .containsExactly(tuple(p1, 3, saved), tuple(p2, 5, saved));

            verify(actionLogService).logAction(eq(MANAGER_ID), eq("CREATE"), eq("Order"), eq(50),
                    isNull(), newStateCaptor.capture());
            assertThat(newStateCaptor.getValue())
                    .containsEntry("number", saved.getOrderNumber())
                    .containsEntry("cost", saved.getOrderCost())
                    .containsEntry("status", "CREATED")
                    .containsEntry("supplier", "Acme Corp")
                    .containsEntry("paymentType", "CARD")
                    .containsEntry("orderDateTime", dto.getOrderDateTime())
                    .containsEntry("estimatedArrivalDate", dto.getEstimatedArrivalDate())
                    .containsEntry("estimatedShipmentDate", dto.getEstimatedShipmentDate());

            verify(supplierRepository, never()).save(any());
            verify(paymentTypeRepository, never()).save(any());
        }

        @Test
        void createsMissingSupplierAndPaymentType() {
            when(supplierRepository.findByCompanyName("New Supplier")).thenReturn(Optional.empty());
            when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderStatusRepository.findByStatusName("CREATED")).thenReturn(Optional.of(orderStatus("CREATED")));
            when(paymentTypeRepository.findByTypeName("CRYPTO")).thenReturn(Optional.empty());
            when(paymentTypeRepository.save(any(PaymentType.class))).thenAnswer(inv -> inv.getArgument(0));
            stubOrderSaveAssigningId(1);

            service.createOrder(orderDto("New Supplier", "CREATED", "CRYPTO"));

            ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
            verify(supplierRepository).save(supplierCaptor.capture());
            assertThat(supplierCaptor.getValue().getCompanyName()).isEqualTo("New Supplier");
            assertThat(supplierCaptor.getValue().getEmail()).isEqualTo("unknown");

            ArgumentCaptor<PaymentType> paymentCaptor = ArgumentCaptor.forClass(PaymentType.class);
            verify(paymentTypeRepository).save(paymentCaptor.capture());
            assertThat(paymentCaptor.getValue().getTypeName()).isEqualTo("CRYPTO");

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getOrderNumber()).startsWith("NEW-");
            verify(orderProductRepository, never()).save(any());
        }

        @Test
        void throws_whenStatusDoesNotExist() {
            when(supplierRepository.findByCompanyName("Acme Corp")).thenReturn(Optional.of(supplier("Acme Corp")));
            when(orderStatusRepository.findByStatusName("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrder(orderDto("Acme Corp", "UNKNOWN", "CARD")))
                    .isInstanceOf(NoSuchElementException.class);

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(actionLogService);
        }

        @Test
        void throws_whenProductDoesNotExist() {
            when(supplierRepository.findByCompanyName("Acme Corp")).thenReturn(Optional.of(supplier("Acme Corp")));
            when(orderStatusRepository.findByStatusName("CREATED")).thenReturn(Optional.of(orderStatus("CREATED")));
            when(paymentTypeRepository.findByTypeName("CARD")).thenReturn(Optional.of(paymentType("CARD")));
            stubOrderSaveAssigningId(5);
            when(concreteProductRepository.findByProductName("Ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrder(orderDto("Acme Corp", "CREATED", "CARD", productDto("Ghost", 1))))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Ghost");

            verify(orderProductRepository, never()).save(any());
            verifyNoInteractions(actionLogService);
        }

        @Test
        void regeneratesOrderNumber_untilUnique() {
            when(supplierRepository.findByCompanyName("Acme Corp")).thenReturn(Optional.of(supplier("Acme Corp")));
            when(orderStatusRepository.findByStatusName("CREATED")).thenReturn(Optional.of(orderStatus("CREATED")));
            when(paymentTypeRepository.findByTypeName("CARD")).thenReturn(Optional.of(paymentType("CARD")));
            when(orderRepository.existsByOrderNumber(anyString())).thenReturn(true, true, false);
            stubOrderSaveAssigningId(5);

            service.createOrder(orderDto("Acme Corp", "CREATED", "CARD"));

            verify(orderRepository, times(3)).existsByOrderNumber(matches("ACM-\\d{3}"));
        }

        @Test
        void rejectsSupplierNameShorterThanTwoCharacters() {
            when(supplierRepository.findByCompanyName("A")).thenReturn(Optional.of(supplier("A")));
            when(orderStatusRepository.findByStatusName("CREATED")).thenReturn(Optional.of(orderStatus("CREATED")));
            when(paymentTypeRepository.findByTypeName("CARD")).thenReturn(Optional.of(paymentType("CARD")));

            assertThatThrownBy(() -> service.createOrder(orderDto("A", "CREATED", "CARD")))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(orderRepository, never()).save(any());
        }

        @Test
        void twoCharacterSupplierName_failsBecauseThreeLetterPrefixIsTaken() {
            // Documents current behaviour: validation allows 2 chars but substring(0, 3) needs 3.
            when(supplierRepository.findByCompanyName("AB")).thenReturn(Optional.of(supplier("AB")));
            when(orderStatusRepository.findByStatusName("CREATED")).thenReturn(Optional.of(orderStatus("CREATED")));
            when(paymentTypeRepository.findByTypeName("CARD")).thenReturn(Optional.of(paymentType("CARD")));

            assertThatThrownBy(() -> service.createOrder(orderDto("AB", "CREATED", "CARD")))
                    .isInstanceOf(StringIndexOutOfBoundsException.class);
        }
    }

    @Nested
    class UpdateOrder {

        @Test
        void throws_whenOrderNotFound() {
            when(orderRepository.findByOrderNumber("NOPE-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateOrder("NOPE-1", orderDto("Acme Corp", "CREATED", "CARD")))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("NOPE-1");
            verify(orderRepository, never()).save(any());
        }

        @Test
        void updatesFields_syncsProducts_andLogsPrevAndNewState() throws Exception {
            Order existing = order(10, "ACM-123", "CREATED");
            ConcreteProduct apple = concreteProduct(1, "Apple");
            ConcreteProduct pear = concreteProduct(2, "Pear");
            ConcreteProduct plum = concreteProduct(3, "Plum");
            OrderProduct appleLine = orderProduct(100, existing, apple, 1);
            OrderProduct pearLine = orderProduct(101, existing, pear, 2);
            Supplier newSupplier = supplier("Globex");
            OrderStatus newStatus = orderStatus("IN_PROGRESS");
            PaymentType newPayment = paymentType("CASH");

            when(orderRepository.findByOrderNumber("ACM-123")).thenReturn(Optional.of(existing));
            when(supplierRepository.findByCompanyName("Globex")).thenReturn(Optional.of(newSupplier));
            when(orderStatusRepository.findByStatusName("IN_PROGRESS")).thenReturn(Optional.of(newStatus));
            when(paymentTypeRepository.findByTypeName("CASH")).thenReturn(Optional.of(newPayment));
            when(orderProductRepository.findOrderProductsByOrderId(10)).thenReturn(List.of(appleLine, pearLine));
            when(concreteProductRepository.findByProductName("Apple")).thenReturn(Optional.of(apple));
            when(concreteProductRepository.findByProductName("Plum")).thenReturn(Optional.of(plum));

            OrderDTO dto = orderDto("Globex", "IN_PROGRESS", "CASH", productDto("Apple", 10), productDto("Plum", 4));
            service.updateOrder("ACM-123", dto);

            verify(orderRepository).save(existing);
            assertThat(existing.getOrderNumber()).isEqualTo("ACM-123");
            assertThat(existing.getOrderCost()).isEqualByComparingTo("2500.00");
            assertThat(existing.getSupplier()).isSameAs(newSupplier);
            assertThat(existing.getOrderStatus()).isSameAs(newStatus);
            assertThat(existing.getPaymentType()).isSameAs(newPayment);
            assertThat(existing.getOrderDateTime()).isEqualTo(dto.getOrderDateTime());
            assertThat(existing.getEstimatedArrivalDate()).isEqualTo(dto.getEstimatedArrivalDate());
            assertThat(existing.getEstimatedShipmentDate()).isEqualTo(dto.getEstimatedShipmentDate());

            ArgumentCaptor<OrderProduct> saved = ArgumentCaptor.forClass(OrderProduct.class);
            verify(orderProductRepository, times(2)).save(saved.capture());
            assertThat(saved.getAllValues().get(0)).isSameAs(appleLine);
            assertThat(appleLine.getProductQuantity()).isEqualTo(10);
            OrderProduct plumLine = saved.getAllValues().get(1);
            assertThat(plumLine.getConcreteProduct()).isSameAs(plum);
            assertThat(plumLine.getProductQuantity()).isEqualTo(4);
            assertThat(plumLine.getOrder()).isSameAs(existing);

            verify(orderProductRepository).delete(pearLine);
            verify(orderProductRepository, never()).delete(appleLine);

            verify(actionLogService).logAction(eq(MANAGER_ID), eq("UPDATE"), eq("Order"), eq(10),
                    prevStateCaptor.capture(), newStateCaptor.capture());
            assertThat(prevStateCaptor.getValue())
                    .containsEntry("number", "ACM-123")
                    .containsEntry("cost", new BigDecimal("1000.00"))
                    .containsEntry("status", "CREATED")
                    .containsEntry("supplier", "Acme Corp")
                    .containsEntry("paymentType", "CARD")
                    .containsEntry("estimatedArrivalDate", LocalDate.of(2025, 3, 10));
            assertThat(newStateCaptor.getValue())
                    .containsEntry("number", "ACM-123")
                    .containsEntry("status", "IN_PROGRESS")
                    .containsEntry("supplier", "Globex")
                    .containsEntry("paymentType", "CASH")
                    .containsEntry("estimatedArrivalDate", LocalDate.of(2025, 4, 20));
        }

        @Test
        void createsMissingSupplierAndPaymentType() throws Exception {
            Order existing = order(10, "ACM-123", "CREATED");
            when(orderRepository.findByOrderNumber("ACM-123")).thenReturn(Optional.of(existing));
            when(supplierRepository.findByCompanyName("Initech")).thenReturn(Optional.empty());
            when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderStatusRepository.findByStatusName("CREATED")).thenReturn(Optional.of(orderStatus("CREATED")));
            when(paymentTypeRepository.findByTypeName("BARTER")).thenReturn(Optional.empty());
            when(paymentTypeRepository.save(any(PaymentType.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderProductRepository.findOrderProductsByOrderId(10)).thenReturn(List.of());

            service.updateOrder("ACM-123", orderDto("Initech", "CREATED", "BARTER"));

            assertThat(existing.getSupplier().getCompanyName()).isEqualTo("Initech");
            assertThat(existing.getSupplier().getEmail()).isEqualTo("unknown");
            assertThat(existing.getPaymentType().getTypeName()).isEqualTo("BARTER");
        }

        @Test
        void throws_whenStatusDoesNotExist() {
            when(orderRepository.findByOrderNumber("ACM-123")).thenReturn(Optional.of(order(10, "ACM-123", "CREATED")));
            when(supplierRepository.findByCompanyName("Acme Corp")).thenReturn(Optional.of(supplier("Acme Corp")));
            when(orderStatusRepository.findByStatusName("BOGUS")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateOrder("ACM-123", orderDto("Acme Corp", "BOGUS", "CARD")))
                    .isInstanceOf(NoSuchElementException.class);
            verify(orderRepository, never()).save(any());
            verifyNoInteractions(actionLogService);
        }

        @Test
        void throws_whenIncomingProductDoesNotExist() {
            Order existing = order(10, "ACM-123", "CREATED");
            when(orderRepository.findByOrderNumber("ACM-123")).thenReturn(Optional.of(existing));
            when(supplierRepository.findByCompanyName("Acme Corp")).thenReturn(Optional.of(supplier("Acme Corp")));
            when(orderStatusRepository.findByStatusName("CREATED")).thenReturn(Optional.of(orderStatus("CREATED")));
            when(paymentTypeRepository.findByTypeName("CARD")).thenReturn(Optional.of(paymentType("CARD")));
            when(orderProductRepository.findOrderProductsByOrderId(10)).thenReturn(List.of());
            when(concreteProductRepository.findByProductName("Ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateOrder("ACM-123",
                    orderDto("Acme Corp", "CREATED", "CARD", productDto("Ghost", 1))))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Ghost");
        }

        private Order stubForRemoval(OrderProduct line) {
            Order existing = line.getOrder();
            when(orderRepository.findByOrderNumber("ACM-123")).thenReturn(Optional.of(existing));
            when(supplierRepository.findByCompanyName("Acme Corp")).thenReturn(Optional.of(supplier("Acme Corp")));
            when(orderStatusRepository.findByStatusName("CREATED")).thenReturn(Optional.of(orderStatus("CREATED")));
            when(paymentTypeRepository.findByTypeName("CARD")).thenReturn(Optional.of(paymentType("CARD")));
            when(orderProductRepository.findOrderProductsByOrderId(10)).thenReturn(List.of(line));
            return existing;
        }

        @Test
        void refusesToRemoveProductWithPlacements() {
            OrderProduct line = orderProduct(100, order(10, "ACM-123", "CREATED"), concreteProduct(1, "Apple"), 1);
            stubForRemoval(line);
            when(productPlacementRepository.existsByOrderProduct(line)).thenReturn(true);

            assertThatThrownBy(() -> service.updateOrder("ACM-123", orderDto("Acme Corp", "CREATED", "CARD")))
                    .isInstanceOf(OperationNotSupportedException.class);
            verify(orderProductRepository, never()).delete(any());
        }

        @Test
        void refusesToRemoveProductWithShipments() {
            OrderProduct line = orderProduct(100, order(10, "ACM-123", "CREATED"), concreteProduct(1, "Apple"), 1);
            stubForRemoval(line);
            when(productShipmentRepository.existsByOrderProduct(line)).thenReturn(true);

            assertThatThrownBy(() -> service.updateOrder("ACM-123", orderDto("Acme Corp", "CREATED", "CARD")))
                    .isInstanceOf(OperationNotSupportedException.class);
            verify(orderProductRepository, never()).delete(any());
        }
    }

    @Nested
    class DeleteOrder {

        @Test
        void throws_whenOrderNotFound() {
            when(orderRepository.findByOrderNumber("X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteOrder("X")).isInstanceOf(NoSuchElementException.class);
            verify(orderRepository, never()).delete(any(Order.class));
        }

        @Test
        void throws_whenStatusIsNotCreated() {
            when(orderRepository.findByOrderNumber("ACM-1")).thenReturn(Optional.of(order(1, "ACM-1", "SHIPPED")));

            assertThatThrownBy(() -> service.deleteOrder("ACM-1"))
                    .isInstanceOf(OperationNotSupportedException.class);
            verify(orderRepository, never()).delete(any(Order.class));
            verifyNoInteractions(orderProductRepository);
        }

        @Test
        void throws_whenProductHasShipment() {
            Order o = order(1, "ACM-1", "CREATED");
            OrderProduct line = orderProduct(5, o, concreteProduct(1, "Apple"), 1);
            when(orderRepository.findByOrderNumber("ACM-1")).thenReturn(Optional.of(o));
            when(orderProductRepository.findOrderProductsByOrderId(1)).thenReturn(List.of(line));
            when(productShipmentRepository.existsByOrderProduct(line)).thenReturn(true);

            assertThatThrownBy(() -> service.deleteOrder("ACM-1")).isInstanceOf(IllegalStateException.class);
            verify(orderProductRepository, never()).deleteAll(any());
            verify(orderRepository, never()).delete(any(Order.class));
        }

        @Test
        void throws_whenProductHasPlacement() {
            Order o = order(1, "ACM-1", "CREATED");
            OrderProduct line = orderProduct(5, o, concreteProduct(1, "Apple"), 1);
            when(orderRepository.findByOrderNumber("ACM-1")).thenReturn(Optional.of(o));
            when(orderProductRepository.findOrderProductsByOrderId(1)).thenReturn(List.of(line));
            when(productPlacementRepository.existsByOrderProduct(line)).thenReturn(true);

            assertThatThrownBy(() -> service.deleteOrder("ACM-1")).isInstanceOf(IllegalStateException.class);
            verify(orderRepository, never()).delete(any(Order.class));
        }

        @Test
        void deletesProductsThenOrder_withoutLogging() throws Exception {
            Order o = order(1, "ACM-1", "CREATED");
            List<OrderProduct> lines = List.of(
                    orderProduct(5, o, concreteProduct(1, "Apple"), 1),
                    orderProduct(6, o, concreteProduct(2, "Pear"), 2));
            when(orderRepository.findByOrderNumber("ACM-1")).thenReturn(Optional.of(o));
            when(orderProductRepository.findOrderProductsByOrderId(1)).thenReturn(lines);

            service.deleteOrder("ACM-1");

            var inOrder = inOrder(orderProductRepository, orderRepository);
            inOrder.verify(orderProductRepository).deleteAll(lines);
            inOrder.verify(orderRepository).delete(o);
            verifyNoInteractions(actionLogService);
        }
    }
}
