package com.vanokal0.warehouse_backend.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.vanokal0.warehouse_backend.utils.LocalDateTimeToInstantDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductShipmentDTO {
    String number;
    String orderNumber;
    String product;
    Integer quantity;
    @JsonDeserialize(using = LocalDateTimeToInstantDeserializer.class)
    Instant shipmentDateTime;
    String roomNumber;
    String lotNumber;
    String lotBookingNumber;
    String employee;
}
