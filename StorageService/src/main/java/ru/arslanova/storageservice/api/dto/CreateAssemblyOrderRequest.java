package ru.arslanova.storageservice.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateAssemblyOrderRequest {
    private UUID sourceOrderId;
}
