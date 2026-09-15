package ru.arslanova.orderservice.domain.Order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderStateMachine {

    private static final Set<OrderStatus> FINAL_STATUSES =
            EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED);

    private static final Map<OrderType, Map<OrderStatus, Set<OrderStatus>>> TRANSITIONS =
            new EnumMap<>(OrderType.class);

    static {
        Map<OrderStatus, Set<OrderStatus>> inStock = new EnumMap<>(OrderStatus.class);
        inStock.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.APPROVED_BY_MANAGER));
        inStock.put(OrderStatus.APPROVED_BY_MANAGER, EnumSet.of(OrderStatus.AWAIT_FOR_PAYMENT));
        inStock.put(OrderStatus.AWAIT_FOR_PAYMENT, EnumSet.of(OrderStatus.PAID));
        inStock.put(OrderStatus.PAID, EnumSet.of(OrderStatus.READY_FOR_PICKUP));
        inStock.put(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.COMPLETED));
        TRANSITIONS.put(OrderType.IN_STOCK, inStock);

        Map<OrderStatus, Set<OrderStatus>> custom = new EnumMap<>(OrderStatus.class);
        custom.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.APPROVED_BY_STORAGE));
        custom.put(OrderStatus.APPROVED_BY_STORAGE, EnumSet.of(OrderStatus.AWAIT_FOR_PAYMENT));
        custom.put(OrderStatus.AWAIT_FOR_PAYMENT, EnumSet.of(OrderStatus.PAID));
        custom.put(OrderStatus.PAID, EnumSet.of(OrderStatus.AWAIT_DELIVERY));
        custom.put(OrderStatus.AWAIT_DELIVERY, EnumSet.of(OrderStatus.READY_FOR_PICKUP));
        custom.put(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.COMPLETED));
        TRANSITIONS.put(OrderType.CUSTOM, custom);

        Map<OrderStatus, Set<OrderStatus>> testDrive = new EnumMap<>(OrderStatus.class);
        testDrive.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.APPROVED_BY_MANAGER));
        testDrive.put(OrderStatus.APPROVED_BY_MANAGER, EnumSet.of(OrderStatus.COMPLETED));
        TRANSITIONS.put(OrderType.TEST_DRIVE, testDrive);
    }

    private OrderStateMachine() {
    }

    public static OrderStatus initialStatus() {
        return OrderStatus.CREATED;
    }

    public static boolean isFinal(OrderStatus status) {
        return FINAL_STATUSES.contains(status);
    }

    public static boolean canTransition(OrderType type, OrderStatus from, OrderStatus to) {
        if (type == null || from == null || to == null) {
            return false;
        }
        if (isFinal(from)) {
            return false;
        }
        if (to == OrderStatus.CANCELLED) {
            return true;
        }
        return allowedFrom(type, from).contains(to);
    }

    public static Set<OrderStatus> allowedFrom(OrderType type, OrderStatus from) {
        Map<OrderStatus, Set<OrderStatus>> byType = TRANSITIONS.get(type);
        if (byType == null || from == null) {
            return EnumSet.noneOf(OrderStatus.class);
        }
        return byType.getOrDefault(from, EnumSet.noneOf(OrderStatus.class));
    }
}
