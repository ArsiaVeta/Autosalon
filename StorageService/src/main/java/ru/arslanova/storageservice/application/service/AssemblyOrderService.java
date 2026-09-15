package ru.arslanova.storageservice.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.AssemblyState;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.CarAssemblyOrder;
import ru.arslanova.storageservice.infrastructure.repository.CarAssemblyOrderRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssemblyOrderService {
    private final CarAssemblyOrderRepository repository;
    public CarAssemblyOrder create(UUID sourceOrderId){
        CarAssemblyOrder order =
                new CarAssemblyOrder(sourceOrderId, AssemblyState.CREATED);
        return repository.save(order);
    }

    public List<CarAssemblyOrder> findAll(){
        return  repository.findAll();
    }

    public CarAssemblyOrder findById(UUID id){
        return repository.findById(id).orElseThrow();
    }

    public CarAssemblyOrder updateState(UUID id, AssemblyState state){
        CarAssemblyOrder order = repository.findById(id).orElseThrow();
        order.setState(state);
        return repository.save(order);
    }

    public void delete(UUID id){
        repository.deleteById(id);
    }

}
