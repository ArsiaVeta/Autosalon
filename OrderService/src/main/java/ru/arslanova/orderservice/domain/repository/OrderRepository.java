package ru.arslanova.orderservice.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.arslanova.orderservice.domain.Order.OrderContext;

import java.util.UUID;

public interface OrderRepository {
    OrderContext save(OrderContext order);
    OrderContext findById(UUID id);
    Page<OrderContext> findAll(Pageable pageable);

    Page<OrderContext> findAllByClientId(UUID clientId, Pageable pageable);
    void deleteById(UUID id);
}
