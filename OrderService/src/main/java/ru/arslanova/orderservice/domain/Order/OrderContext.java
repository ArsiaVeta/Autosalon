package ru.arslanova.orderservice.domain.Order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.arslanova.orderservice.domain.exeptions.DomainValidationExeption;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderContext {
    private UUID id;
    private UUID clientId;
    private UUID managerId;
    private UUID carModelId;
    private OrderType type;
    private OrderStatus status;
    private Map<String, UUID> components = new HashMap<>();


    public OrderContext(UUID clientId, UUID managerId, UUID carModelId,
                        OrderType type, OrderStatus status, Map<String, UUID> components) {
        this.id = UUID.randomUUID();
        this.clientId = clientId;
        this.managerId = managerId;
        this.carModelId = carModelId;
        this.type = type;
        this.status = status;
        this.components = components != null ? components : new HashMap<>();
    }

    public boolean canTransitionTo(OrderStatus next) {
        return OrderStateMachine.canTransition(type, status, next);
    }

    public void transitionTo(OrderStatus next) {
        if (!canTransitionTo(next)) {
            throw new DomainValidationExeption(
                    "Illegal status transition for " + type + " order: " + status + " -> " + next);
        }
        this.status = next;
    }

    public boolean isFinal() {
        return OrderStateMachine.isFinal(status);
    }
}
