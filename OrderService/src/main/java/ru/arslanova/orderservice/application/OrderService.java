package ru.arslanova.orderservice.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.domain.Order.OrderStateMachine;
import ru.arslanova.orderservice.domain.Order.OrderStatus;
import ru.arslanova.orderservice.domain.Order.OrderType;
import ru.arslanova.orderservice.domain.exeptions.DomainValidationExeption;
import ru.arslanova.orderservice.domain.repository.OrderRepository;
import ru.arslanova.orderservice.event.OrderPhase;
import ru.arslanova.orderservice.event.OrderSentForApprovalEvent;
import ru.arslanova.orderservice.infrastructure.security.SecurityUtils;
import ru.arslanova.orderservice.outbox.OutboxService;


import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final OutboxService outboxService;
    private final ManagerAssignmentService managerAssignmentService;

    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public OrderContext create(OrderContext order){
        if (order.getCarModelId() == null){
            throw new DomainValidationExeption("carModelId is required");
        }

        try{
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (order.getClientId() == null){
                order.setClientId(currentUserId);
            }
        } catch (Exception ignored){}

        if (order.getClientId() == null){
            throw new DomainValidationExeption("clientId is required");
        }

        order.setManagerId(managerAssignmentService.assign(order.getId()));
        order.setStatus(OrderStateMachine.initialStatus());

        OrderContext saved = repository.save(order);

        if (saved.getType() == OrderType.CUSTOM){
            publish(saved, OrderPhase.APPROVAL);
        }

        log.info("ORDER CREATED orderId={} type={} clientId={} managerId={}",
                saved.getId(), saved.getType(), saved.getClientId(), saved.getManagerId());

        return saved;
    }

    @Transactional
    @PreAuthorize("""
        hasRole('ADMIN')
        or hasRole('MANAGER')
        or @orderSecurity.isOwner(#id)
    """)
    public OrderContext findById(UUID id){
        return repository.findById(id);
    }

    public Page<OrderContext> findAll(Pageable pageable){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isManager = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isManager || isAdmin){
            return repository.findAll(pageable);
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return repository.findAllByClientId(currentUserId, pageable);
    }

    @Transactional
    @PreAuthorize("""
        hasRole('ADMIN')
        or hasRole('MANAGER')
        or @orderSecurity.isOwner(#id)
    """)
    public void delete(UUID id){ repository.deleteById(id); }

    @Transactional
    @PreAuthorize("""
        hasRole('ADMIN')
        or @orderSecurity.isAssignedManager(#orderId)
    """)
    public OrderContext approveByManager(UUID orderId){
        OrderContext order = repository.findById(orderId);

        if (order.getType() != OrderType.IN_STOCK && order.getType() != OrderType.TEST_DRIVE){
            throw new DomainValidationExeption(
                    "Order of type " + order.getType() + " is approved by storage, not by manager");
        }

        order.transitionTo(OrderStatus.APPROVED_BY_MANAGER);
        return save(order, "APPROVED BY MANAGER");
    }

    @Transactional
    @PreAuthorize("""
        hasRole('ADMIN')
        or @orderSecurity.isAssignedManager(#orderId)
    """)
    public OrderContext requestPayment(UUID orderId){
        OrderContext order = repository.findById(orderId);
        order.transitionTo(OrderStatus.AWAIT_FOR_PAYMENT);
        return save(order, "AWAITING PAYMENT");
    }

    @Transactional
    @PreAuthorize("""
        hasRole('ADMIN')
        or @orderSecurity.isAssignedManager(#orderId)
        or @orderSecurity.isOwner(#orderId)
    """)
    public OrderContext pay(UUID orderId){
        OrderContext order = repository.findById(orderId);
        order.transitionTo(OrderStatus.PAID);
        OrderContext saved = save(order, "PAID");

        publish(saved, OrderPhase.ASSEMBLY);

        return saved;
    }

    @Transactional
    @PreAuthorize("""
        hasRole('ADMIN')
        or @orderSecurity.isAssignedManager(#orderId)
    """)
    public OrderContext complete(UUID orderId){
        OrderContext order = repository.findById(orderId);
        order.transitionTo(OrderStatus.COMPLETED);
        return save(order, "COMPLETED");
    }

    @Transactional
    public void approveByStorage(UUID orderId){
        applyEventTransition(orderId, OrderStatus.APPROVED_BY_STORAGE);
    }

    @Transactional
    public void markAwaitingDelivery(UUID orderId){
        applyEventTransition(orderId, OrderStatus.AWAIT_DELIVERY);
    }

    @Transactional
    public void markReadyForPickup(UUID orderId){
        applyEventTransition(orderId, OrderStatus.READY_FOR_PICKUP);
    }

    @Transactional
    @PreAuthorize("""
        hasRole('ADMIN')
        or hasRole('MANAGER')
        or @orderSecurity.isOwner(#orderId)
    """)
    public OrderContext cancel(UUID orderId){
        OrderContext order = repository.findById(orderId);
        order.transitionTo(OrderStatus.CANCELLED);
        return save(order, "CANCELLED");
    }

    @Transactional
    public void cancelByStorage(UUID orderId){
        applyEventTransition(orderId, OrderStatus.CANCELLED);
    }

    private void applyEventTransition(UUID orderId, OrderStatus next){
        OrderContext order = repository.findById(orderId);

        if (!order.canTransitionTo(next)){
            log.warn("SKIPPED STATUS TRANSITION orderId={} type={} {} -> {}",
                    orderId, order.getType(), order.getStatus(), next);
            return;
        }

        order.transitionTo(next);
        save(order, next.name());
    }

    private OrderContext save(OrderContext order, String action){
        OrderContext saved = repository.save(order);
        log.info("ORDER {} orderId={} type={} status={}",
                action, saved.getId(), saved.getType(), saved.getStatus());
        return saved;
    }

    private void publish(OrderContext order, OrderPhase phase){
        OrderSentForApprovalEvent event = new OrderSentForApprovalEvent();

        event.setEventId(UUID.randomUUID());
        event.setOrderId(order.getId());
        event.setCarModelId(order.getCarModelId());
        event.setOrderType(order.getType());
        event.setComponents(order.getComponents());
        event.setPhase(phase);

        String traceId = org.slf4j.MDC.get("traceId");
        if (traceId == null || traceId.isBlank()){
            traceId = UUID.randomUUID().toString();
        }
        event.setTraceId(traceId);
        event.setCreatedAt(Instant.now());

        outboxService.saveEvent(event);
    }
}
