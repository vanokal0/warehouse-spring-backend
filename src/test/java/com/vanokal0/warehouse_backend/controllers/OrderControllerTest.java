package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.OrderDTO;
import com.vanokal0.warehouse_backend.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.naming.OperationNotSupportedException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private static final String BODY = """
            {
              "supplier": "Acme Corp",
              "orderStatus": "CREATED",
              "paymentType": "CARD",
              "cost": 2500.00,
              "orderDateTime": "2025-04-01T08:00:00Z",
              "estimatedArrivalDate": "2025-04-20",
              "estimatedShipmentDate": "2025-04-10",
              "products": [
                { "concreteProduct": { "concreteProductName": "Apple" }, "quantity": 3 }
              ]
            }
            """;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getPaged_passesAllQueryParameters() throws Exception {
        when(orderService.getPagedOrders(anyInt(), anyInt(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(Map.of("items", List.of(), "total", 0L));

        mockMvc.perform(get("/api/orders/paged")
                        .param("page", "2").param("pageSize", "25")
                        .param("sortField", "cost").param("sortOrder", "desc")
                        .param("filterField", "supplier").param("filterField", "cost")
                        .param("filterOperator", "contains").param("filterOperator", ">")
                        .param("filterValue", "acme").param("filterValue", "100")
                        .param("all", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isArray());

        verify(orderService).getPagedOrders(2, 25,
                List.of("cost"), List.of("desc"),
                List.of("supplier", "cost"), List.of("contains", ">"), List.of("acme", "100"),
                true);
    }

    @Test
    void getPaged_usesDefaults() throws Exception {
        when(orderService.getPagedOrders(anyInt(), anyInt(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(Map.of());

        mockMvc.perform(get("/api/orders/paged")).andExpect(status().isOk());

        verify(orderService).getPagedOrders(0, 10, null, null, null, null, null, false);
    }

    @Test
    void create_returns201WithCreatedOrder() throws Exception {
        OrderDTO created = new OrderDTO();
        created.setNumber("ACM-123");
        when(orderService.createOrder(any(OrderDTO.class))).thenReturn(created);

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("ACM-123"));

        ArgumentCaptor<OrderDTO> captor = ArgumentCaptor.forClass(OrderDTO.class);
        verify(orderService).createOrder(captor.capture());
        OrderDTO sent = captor.getValue();
        assertThat(sent.getSupplier()).isEqualTo("Acme Corp");
        assertThat(sent.getCost()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(sent.getOrderDateTime()).isEqualTo(Instant.parse("2025-04-01T08:00:00Z"));
        assertThat(sent.getEstimatedArrivalDate()).isEqualTo(LocalDate.of(2025, 4, 20));
        assertThat(sent.getProducts()).hasSize(1);
        assertThat(sent.getProducts().get(0).getConcreteProduct().getConcreteProductName()).isEqualTo("Apple");
        assertThat(sent.getProducts().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void create_returns500WithMessage_onFailure() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new IllegalArgumentException("boom"));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("boom")));
    }

    @Test
    void update_returns200_onSuccess() throws Exception {
        mockMvc.perform(put("/api/orders/ACM-123").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        verify(orderService).updateOrder(eq("ACM-123"), any(OrderDTO.class));
    }

    @Test
    void update_returns404_whenOrderMissing() throws Exception {
        doThrow(new NoSuchElementException("missing ACM-123"))
                .when(orderService).updateOrder(eq("ACM-123"), any());

        mockMvc.perform(put("/api/orders/ACM-123").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("missing ACM-123")));
    }

    @Test
    void update_returns500_onOtherErrors() throws Exception {
        doThrow(new OperationNotSupportedException("has operations"))
                .when(orderService).updateOrder(eq("ACM-123"), any());

        mockMvc.perform(put("/api/orders/ACM-123").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("has operations")));
    }

    @Test
    void delete_returns204_onSuccess() throws Exception {
        mockMvc.perform(delete("/api/orders/ACM-123")).andExpect(status().isNoContent());

        verify(orderService).deleteOrder("ACM-123");
    }

    @Test
    void delete_returns500_onFailure() throws Exception {
        doThrow(new IllegalStateException("linked")).when(orderService).deleteOrder("ACM-123");

        mockMvc.perform(delete("/api/orders/ACM-123"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("linked")));
    }
}
