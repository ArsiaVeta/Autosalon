package ru.arslanova.orderservice.infrastructure.mappers;

import org.springframework.stereotype.Component;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.infrastructure.entity.OrderEntity;


@Component
public class OrderPersistenceMapper {

    public OrderEntity toEntity(OrderContext order){
        return new OrderEntity(
                order.getId(),
                order.getClientId(),
                order.getManagerId(),
                order.getCarModelId(),
                order.getType(),
                order.getStatus(),
                order.getComponents()
        );
    }

    public OrderContext toDomain(OrderEntity entity){
        return new OrderContext(
                entity.getId(),
                entity.getClientId(),
                entity.getManagerId(),
                entity.getCarModelId(),
                entity.getType(),
                entity.getStatus(),
                entity.getComponents()
        );
    }

}