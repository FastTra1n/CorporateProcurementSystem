package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.Warehouse;

import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        String name,
        String location
) {
    public static WarehouseResponse of(Warehouse warehouse) {
        return new WarehouseResponse(warehouse.getId(), warehouse.getName(), warehouse.getLocation());
    }
}
