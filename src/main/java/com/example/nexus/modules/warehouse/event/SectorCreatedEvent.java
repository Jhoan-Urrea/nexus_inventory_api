package com.example.nexus.modules.warehouse.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SectorCreatedEvent extends ApplicationEvent {
    private final Long sectorId;

    public SectorCreatedEvent(Object source, Long sectorId) {
        super(source);
        this.sectorId = sectorId;
    }
}
