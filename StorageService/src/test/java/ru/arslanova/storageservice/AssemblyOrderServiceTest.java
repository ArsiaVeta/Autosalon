package ru.arslanova.storageservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.arslanova.storageservice.application.service.AssemblyOrderService;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.AssemblyState;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.CarAssemblyOrder;
import ru.arslanova.storageservice.infrastructure.repository.CarAssemblyOrderRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AssemblyOrderServiceTest {
    @Mock
    CarAssemblyOrderRepository repository;
    @InjectMocks
    AssemblyOrderService service;

    @Test
    void createShouldPersistOrderInCreatedState() {
        UUID sourceOrderId = UUID.randomUUID();
        when(repository.save(any(CarAssemblyOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CarAssemblyOrder created = service.create(sourceOrderId);

        ArgumentCaptor<CarAssemblyOrder> captor = ArgumentCaptor.forClass(CarAssemblyOrder.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getSourceOrderId()).isEqualTo(sourceOrderId);
        assertThat(captor.getValue().getState()).isEqualTo(AssemblyState.CREATED);
        assertThat(created.getSourceOrderId()).isEqualTo(sourceOrderId);
    }

    @Test
    void updateStateShouldTransitionState() {
        UUID id = UUID.randomUUID();
        CarAssemblyOrder order = new CarAssemblyOrder(UUID.randomUUID(), AssemblyState.CREATED);
        when(repository.findById(id)).thenReturn(Optional.of(order));
        when(repository.save(any(CarAssemblyOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateState(id, AssemblyState.ASSEMBLED);

        assertThat(order.getState()).isEqualTo(AssemblyState.ASSEMBLED);
    }
}
