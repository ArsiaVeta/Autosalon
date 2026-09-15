package ru.arslanova.orderservice.infrastructure.entity;


import jakarta.persistence.*;
import lombok.*;
import ru.arslanova.orderservice.domain.Order.OrderStatus;
import ru.arslanova.orderservice.domain.Order.OrderType;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@Setter
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID clientId;

    @Column
    private UUID managerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private UUID carModelId;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "order_components",
            joinColumns = @JoinColumn(name = "order_id")
    )
    @MapKeyColumn(name = "component_type")
    @Column(name = "part_id")
    private Map<String, UUID> components = new HashMap<>();

    public OrderEntity(UUID id, UUID clientId, UUID managerId, UUID carModelId,
                       OrderType type, OrderStatus status, Map<String, UUID> components){
        this.setId(id);
        this.clientId = clientId;
        this.managerId = managerId;
        this.carModelId = carModelId;
        this.type = type;
        this.status = status;
        this.components = components != null ? components : new HashMap<>();
    }
}
