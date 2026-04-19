package com.example.nexus.modules.warehouse.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WarehouseCreatedEvent extends ApplicationEvent {
    private final Long warehouseId;

    public WarehouseCreatedEvent(Object source, Long warehouseId) {
        super(source);
        this.warehouseId = warehouseId;
    }
}
