package ru.arslanova.orderservice.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.domain.repository.OrderRepository;

import java.util.UUID;

@Component("orderSecurity")
@RequiredArgsConstructor
public class OrderSecurity {

    private final OrderRepository repository;

    public boolean isOwner(UUID orderId) {

        try {
            UUID userId = SecurityUtils.getCurrentUserId();

            OrderContext context = repository.findById(orderId);

            if (context == null || context.getClientId() == null) {
                return false;
            }

            return context.getClientId().equals(userId);

        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAssignedManager(UUID orderId) {

        try {
            if (!SecurityUtils.hasRole("MANAGER")) {
                return false;
            }

            UUID userId = SecurityUtils.getCurrentUserId();

            OrderContext context = repository.findById(orderId);

            if (context == null || context.getManagerId() == null) {
                return false;
            }

            return context.getManagerId().equals(userId);

        } catch (Exception e) {
            return false;
        }
    }
}
