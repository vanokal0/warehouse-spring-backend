package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.ActionLogDTO;
import com.vanokal0.warehouse_backend.services.ActionLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ActionLogControllerTest {

    @Mock
    private ActionLogService actionLogService;

    @InjectMocks
    private ActionLogController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static ActionLogDTO dto(String id) {
        ActionLogDTO dto = new ActionLogDTO();
        dto.setId(id);
        return dto;
    }

    @Test
    void getAllLogs_returnsLogs() throws Exception {
        when(actionLogService.getAllLogs()).thenReturn(List.of(dto("a"), dto("b")));

        mockMvc.perform(get("/api/action-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("a"));
    }

    @Test
    void getLogsByUser_returnsUserLogs() throws Exception {
        when(actionLogService.getLogsByUserId(110)).thenReturn(List.of(dto("x")));

        mockMvc.perform(get("/api/action-logs/by-user/110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("x"));
    }

    @Test
    void getLogsByUser_rejectsNonNumericId() throws Exception {
        mockMvc.perform(get("/api/action-logs/by-user/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void cancel_returns200_whenCancelled() throws Exception {
        when(actionLogService.cancelAction("log-1")).thenReturn(true);

        mockMvc.perform(post("/api/action-logs/cancel/log-1")).andExpect(status().isOk());
    }

    @Test
    void cancel_returns400_whenServiceRefuses() throws Exception {
        when(actionLogService.cancelAction("log-1")).thenReturn(false);

        mockMvc.perform(post("/api/action-logs/cancel/log-1")).andExpect(status().isBadRequest());
    }

    @Test
    void cancel_propagatesMissingLog_becauseNoHandlerIsDefined() {
        when(actionLogService.cancelAction("nope")).thenThrow(new NoSuchElementException("Action not found"));

        assertThatThrownBy(() -> mockMvc.perform(post("/api/action-logs/cancel/nope")))
                .hasRootCauseInstanceOf(NoSuchElementException.class);
    }
}
