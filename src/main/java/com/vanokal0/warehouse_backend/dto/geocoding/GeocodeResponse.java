package com.vanokal0.warehouse_backend.dto.geocoding;

import lombok.Data;

import java.util.List;

@Data
public class GeocodeResponse {
    private List<GeocodeResult> results;
    private String status;
}
