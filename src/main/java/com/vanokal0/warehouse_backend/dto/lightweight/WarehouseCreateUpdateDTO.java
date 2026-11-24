package com.vanokal0.warehouse_backend.dto.lightweight;

import lombok.Data;

@Data
public class WarehouseCreateUpdateDTO {
    private Integer id;
    private String number;
    private String address;
}
