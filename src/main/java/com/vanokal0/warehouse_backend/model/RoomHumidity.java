package com.vanokal0.warehouse_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class RoomHumidity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_humidity_id", nullable = false)
    private Integer id;

    @Column(name = "humidity", nullable = false)
    private Integer humidity;

}