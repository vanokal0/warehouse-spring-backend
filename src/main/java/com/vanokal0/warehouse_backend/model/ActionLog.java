package com.vanokal0.warehouse_backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "action_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ActionLog {
    @Id
    private String id;

    private Integer userId;
    private String action;
    private LocalDateTime timestamp;

    private String entityType;
    private Integer entityId;


    private Map<String, Object> previousState;
    private Map<String, Object> newState;
    private boolean isCancelled;
}