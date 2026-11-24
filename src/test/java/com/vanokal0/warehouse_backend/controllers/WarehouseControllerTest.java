package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.WarehouseDTO;
import com.vanokal0.warehouse_backend.dto.lightweight.WarehouseCreateUpdateDTO;
import com.vanokal0.warehouse_backend.services.EmployeeService;
import com.vanokal0.warehouse_backend.services.RoomService;
import com.vanokal0.warehouse_backend.services.WarehouseService;
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

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WarehouseControllerTest {

    private static final String BODY = "{\"number\":\"MA9\",\"address\":\"New street 1\"}";

    @Mock
    private RoomService roomService;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private WarehouseService warehouseService;

    @InjectMocks
    private WarehouseController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static WarehouseDTO dto(int id, String number) {
        WarehouseDTO dto = new WarehouseDTO();
        dto.setId(id);
        dto.setNumber(number);
        return dto;
    }

    @Test
    void getAll_returnsWarehouses() throws Exception {
        when(warehouseService.getAllWarehouses()).thenReturn(List.of(dto(1, "MA1"), dto(2, "MA2")));

        mockMvc.perform(get("/api/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].number").value("MA2"));
    }

    @Test
    void getByNumber_returnsWarehouse() throws Exception {
        when(warehouseService.getWarehouseByNumber("MA1")).thenReturn(dto(1, "MA1"));

        mockMvc.perform(get("/api/warehouses/MA1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getByNumber_propagatesNotFound_becauseNoHandlerIsDefined() {
        when(warehouseService.getWarehouseByNumber("X")).thenThrow(new NoSuchElementException("nope"));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/warehouses/X")))
                .hasRootCauseInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getOperators_delegatesToEmployeeService() throws Exception {
        when(employeeService.getOperatorFullNamesByWarehouse("MA1")).thenReturn(List.of("Ann Lee", "Bob Ray"));

        mockMvc.perform(get("/api/warehouses/operators").param("warehouseNumber", "MA1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Ann Lee"))
                .andExpect(jsonPath("$[1]").value("Bob Ray"));
    }

    @Test
    void getOperators_requiresWarehouseNumber() throws Exception {
        mockMvc.perform(get("/api/warehouses/operators")).andExpect(status().isBadRequest());
        verifyNoInteractions(employeeService);
    }

    @Test
    void getRooms_delegatesToRoomService() throws Exception {
        when(roomService.getAvailableRoomNumbersByWarehouseAndType("MA1", "Food")).thenReturn(List.of("R1"));

        mockMvc.perform(get("/api/warehouses/rooms")
                        .param("warehouseNumber", "MA1").param("productType", "Food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("R1"));
    }

    @Test
    void create_returns200WithWarehouse() throws Exception {
        when(warehouseService.createWarehouse(any())).thenReturn(dto(9, "MA9"));

        mockMvc.perform(post("/api/warehouses").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("MA9"));

        ArgumentCaptor<WarehouseCreateUpdateDTO> captor = ArgumentCaptor.forClass(WarehouseCreateUpdateDTO.class);
        verify(warehouseService).createWarehouse(captor.capture());
        assertThat(captor.getValue().getNumber()).isEqualTo("MA9");
        assertThat(captor.getValue().getAddress()).isEqualTo("New street 1");
    }

    @Test
    void create_returns500_onFailure() throws Exception {
        when(warehouseService.createWarehouse(any())).thenThrow(new NoSuchElementException("duplicate"));

        mockMvc.perform(post("/api/warehouses").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("duplicate")));
    }

    @Test
    void update_returns200WithWarehouse() throws Exception {
        when(warehouseService.updateWarehouse(eq(9), any())).thenReturn(dto(9, "MA9"));

        mockMvc.perform(put("/api/warehouses/9").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void update_returns500_onFailure() throws Exception {
        when(warehouseService.updateWarehouse(eq(9), any())).thenThrow(new NoSuchElementException("missing"));

        mockMvc.perform(put("/api/warehouses/9").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("missing")));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/warehouses/9")).andExpect(status().isNoContent());

        verify(warehouseService).archiveWarehouse(9);
    }

    @Test
    void delete_returns500_onFailure() throws Exception {
        doThrow(new NoSuchElementException("missing")).when(warehouseService).archiveWarehouse(9);

        mockMvc.perform(delete("/api/warehouses/9"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("missing")));
    }
}
