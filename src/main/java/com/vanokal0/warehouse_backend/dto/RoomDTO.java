package com.vanokal0.warehouse_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomDTO {
    String number;
    String warehouseNumber;
    Integer availableSpace;
    String temperature;
    Integer humidity;
    String type;
}
