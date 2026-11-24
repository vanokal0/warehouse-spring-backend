package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.model.ApplicationUser;
import com.vanokal0.warehouse_backend.model.Employee;
import com.vanokal0.warehouse_backend.model.EmployeePosition;
import com.vanokal0.warehouse_backend.model.Warehouse;
import com.vanokal0.warehouse_backend.services.ApplicationUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private ApplicationUserService userService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void login_returnsEmployeeProfile_onValidCredentials() throws Exception {
        Warehouse warehouse = new Warehouse();
        warehouse.setNumber("MA224");
        EmployeePosition position = new EmployeePosition();
        position.setPositionName("Warehouse Manager");
        Employee employee = new Employee();
        employee.setId(7);
        employee.setFirstName("James");
        employee.setLastName("Smith");
        employee.setWarehouse(warehouse);
        employee.setPosition(position);
        ApplicationUser user = new ApplicationUser();
        user.setEmployee(employee);

        when(userService.authenticate("jsmith", "password")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"jsmith\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("James Smith"))
                .andExpect(jsonPath("$.employeeId").value(7))
                .andExpect(jsonPath("$.warehouseNumber").value("MA224"))
                .andExpect(jsonPath("$.role").value("Warehouse Manager"));
    }

    @Test
    void login_returns401_onInvalidCredentials() throws Exception {
        when(userService.authenticate("jsmith", "bad")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"jsmith\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns401_whenCredentialsMissing() throws Exception {
        when(userService.authenticate(null, null)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
