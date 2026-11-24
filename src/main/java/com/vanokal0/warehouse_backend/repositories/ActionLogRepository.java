package com.vanokal0.warehouse_backend.repositories;

import com.vanokal0.warehouse_backend.model.ActionLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActionLogRepository extends MongoRepository<ActionLog, String> {
    List<ActionLog> findByUserId(Integer userId);
}
