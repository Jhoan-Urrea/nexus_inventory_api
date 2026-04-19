package com.example.nexus.modules.warehouse.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StorageSpaceCreatedEvent extends ApplicationEvent {
    private final Long storageSpaceId;

    public StorageSpaceCreatedEvent(Object source, Long storageSpaceId) {
        super(source);
        this.storageSpaceId = storageSpaceId;
    }
}
