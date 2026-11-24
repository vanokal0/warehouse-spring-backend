package com.vanokal0.warehouse_backend.dto.geocoding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeocodeLocation {
    private double lat;
    private double lng;
}
