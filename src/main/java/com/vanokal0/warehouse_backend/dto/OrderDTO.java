package com.vanokal0.warehouse_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.vanokal0.warehouse_backend.utils.LocalDateTimeToInstantDeserializer;
import com.vanokal0.warehouse_backend.utils.LocalDateToLocalDateSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    String number;
    List<OrderProductDTO> products;
    BigDecimal cost;

    Instant orderDateTime;

    LocalDate estimatedArrivalDate;

    LocalDate estimatedShipmentDate;
    String paymentType;
    String orderStatus;
    String supplier;
}
