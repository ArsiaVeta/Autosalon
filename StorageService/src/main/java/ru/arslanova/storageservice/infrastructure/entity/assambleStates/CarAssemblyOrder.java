package ru.arslanova.storageservice.infrastructure.entity.assambleStates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.arslanova.storageservice.infrastructure.entity.BaseEntity;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "assembly_orders")
public class CarAssemblyOrder extends BaseEntity {

    @Column(nullable = false)
    public UUID sourceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AssemblyState state;

    public CarAssemblyOrder(UUID sourceOrderId, AssemblyState state){
        this.setId(UUID.randomUUID());
        this.sourceOrderId = sourceOrderId;
        this.state = state;
    }
}
