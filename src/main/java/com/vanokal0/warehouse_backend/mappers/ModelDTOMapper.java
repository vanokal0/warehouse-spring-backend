package com.vanokal0.warehouse_backend.mappers;

public interface ModelDTOMapper<Model, DTO> {
    DTO toDTO(Model model);
}
