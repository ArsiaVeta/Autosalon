package ru.arslanova.orderservice.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.domain.exeptions.EntityNotFoundException;
import ru.arslanova.orderservice.domain.repository.OrderRepository;
import ru.arslanova.orderservice.infrastructure.mappers.OrderPersistenceMapper;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaCustomOrderRepositoryAdapter implements OrderRepository {
    private final JpaOrderRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    @Override
    public OrderContext save(OrderContext order){
        var entity = mapper.toEntity(order);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public OrderContext findById(UUID id){
        return jpaRepository.findById(id)
                .map(mapper::toDomain)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

    }

    @Override
    public Page<OrderContext> findAll(Pageable pageable){
        return jpaRepository.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<OrderContext> findAllByClientId(UUID clientId, Pageable pageable) {
        return jpaRepository.findAllByClientId(clientId, pageable)
                .map(mapper::toDomain);
    }

    @Override
   public void deleteById(UUID id){
        jpaRepository.deleteById(id);
    }
}
