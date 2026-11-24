package com.vanokal0.warehouse_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDTO {
    Integer id;
    String firstName;
    String lastName;
    String fullName;
    String position;
    String warehouseNumber;
}
