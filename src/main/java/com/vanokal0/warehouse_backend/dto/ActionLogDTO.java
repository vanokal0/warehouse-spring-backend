package com.vanokal0.warehouse_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActionLogDTO {
    String id;
    String initiatorFullName;
    LocalDateTime timestamp;
    boolean isCancelled;

    String operationType;

    String entityName;
    String entityType;

    Map<String, String> prevAttributes;
    Map<String, String> newAttributes;
}
