package com.vanokal0.warehouse_backend.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.vanokal0.warehouse_backend.utils.LocalDateTimeToInstantDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LotBookingDTO {
    String number;

    String lotNumber;
    String supplierName;

    @JsonDeserialize(using = LocalDateTimeToInstantDeserializer.class)
    Instant bookingDateTime;
    @JsonDeserialize(using = LocalDateTimeToInstantDeserializer.class)
    Instant startDateTime;
    @JsonDeserialize(using = LocalDateTimeToInstantDeserializer.class)
    Instant endDateTime;
}
