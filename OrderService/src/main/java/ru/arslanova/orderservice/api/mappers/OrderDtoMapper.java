package ru.arslanova.orderservice.api.mappers;

import org.springframework.stereotype.Component;
import ru.arslanova.orderservice.api.dto.CreateOrderRequest;
import ru.arslanova.orderservice.api.dto.OrderResponse;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.domain.Order.OrderStateMachine;
import ru.arslanova.orderservice.domain.Order.OrderType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderDtoMapper {
    public OrderResponse toResponse(OrderContext order){
        return new OrderResponse(
                order.getId(),
                order.getClientId(),
                order.getManagerId(),
                order.getCarModelId(),
                order.getType() != null ? order.getType().name() : null,
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getComponents()
        );
    }

    public OrderContext toDomain(CreateOrderRequest dto) {
        OrderType type;
        try{
            type = OrderType.valueOf(dto.getType().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknow order type: " + dto.getType());
        }

        Map<String, UUID> components =
                dto.getComponents() != null ? new HashMap<>(dto.getComponents()) : new HashMap<>();

        return new OrderContext(
                dto.getClientId(),
                null,
                dto.getCarModelId(),
                type,
                OrderStateMachine.initialStatus(),
                components
        );
    }
}
