package ru.arslanova.storageservice.api.mappers;

import org.mapstruct.Mapper;
import ru.arslanova.storageservice.api.dto.AssemblyOrderResponse;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.CarAssemblyOrder;

@Mapper(componentModel = "spring")
public interface AssemblyOrderMapper {
    default AssemblyOrderResponse toResponse(CarAssemblyOrder order){
        return new AssemblyOrderResponse(
                order.getId(),
                order.getSourceOrderId(),
                order.getState().name()
        );
    }
}
