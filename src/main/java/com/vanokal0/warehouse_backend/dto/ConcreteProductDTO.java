package com.vanokal0.warehouse_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConcreteProductDTO {
    Integer id;
    String concreteProductName;
    String productName;
    String type;
    BigDecimal value;
    Double weight;
    String manufacturer;
}