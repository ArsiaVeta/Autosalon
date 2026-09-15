package ru.arslanova.orderservice.domain.Order;

public enum OrderStatus {
    CREATED,
    APPROVED_BY_MANAGER,
    APPROVED_BY_STORAGE,
    AWAIT_FOR_PAYMENT,
    PAID,
    AWAIT_DELIVERY,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED
}
