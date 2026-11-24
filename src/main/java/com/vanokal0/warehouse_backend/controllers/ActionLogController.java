package com.vanokal0.warehouse_backend.controllers;

import com.vanokal0.warehouse_backend.dto.ActionLogDTO;
import com.vanokal0.warehouse_backend.model.ActionLog;
import com.vanokal0.warehouse_backend.services.ActionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/action-logs")
public class ActionLogController {

    private final ActionLogService actionLogService;

    public ActionLogController(ActionLogService actionLogService) {
        this.actionLogService = actionLogService;
    }

    @GetMapping
    public ResponseEntity<List<ActionLogDTO>> getAllLogs() {
        return ResponseEntity.ok(actionLogService.getAllLogs());
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<ActionLogDTO>> getLogsByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(actionLogService.getLogsByUserId(userId));
    }

    @PostMapping("/cancel/{logId}")
    public ResponseEntity<?> cancelAction(@PathVariable String logId) {
        boolean success = actionLogService.cancelAction(logId);
        if (success) {
            return ResponseEntity.ok("Операцію скасовано.");
        } else {
            return ResponseEntity.badRequest().body("Лог не знайдено або не може бути скасовано.");
        }
    }
}
