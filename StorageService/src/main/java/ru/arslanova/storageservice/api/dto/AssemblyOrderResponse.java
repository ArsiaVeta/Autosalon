package ru.arslanova.storageservice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AssemblyOrderResponse {
    private UUID id;
    private UUID sourceOrderId;
    private String state;
}
