package com.vanokal0.warehouse_backend.dto.geocoding;

import lombok.Data;

@Data
public class GeocodeResult {
    private GeocodeGeometry geometry;
    private String formattedAddress;
    private String placeId;
}
