package ru.arslanova.storageservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import ru.arslanova.storageservice.config.JpaConfig;
import ru.arslanova.storageservice.infrastructure.entity.ProcessedMessageEntity;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.AssemblyState;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.CarAssemblyOrder;
import ru.arslanova.storageservice.infrastructure.repository.CarAssemblyOrderRepository;
import ru.arslanova.storageservice.infrastructure.repository.ProcessedMessageRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false"
})
public class AssemblyOrderRepositoryIntegrationTest extends AbstractPostgresIntegrationTest
{
    @Autowired
    CarAssemblyOrderRepository repository;
    @Autowired
    ProcessedMessageRepository processedRepository;

    @Test
    void shouldPersistAssemblyOrderAndUpdateState() {
        UUID sourceOrderId = UUID.randomUUID();
        CarAssemblyOrder order = new CarAssemblyOrder(sourceOrderId, AssemblyState.CREATED);

        CarAssemblyOrder saved = repository.save(order);
        assertThat(saved.getId()).isNotNull();

        saved.setState(AssemblyState.ASSEMBLED);
        repository.save(saved);

        Optional<CarAssemblyOrder> loaded = repository.findById(saved.getId());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getState()).isEqualTo(AssemblyState.ASSEMBLED);
        assertThat(loaded.get().getSourceOrderId()).isEqualTo(sourceOrderId);
    }

    @Test
    void existsByEventIdShouldDetectProcessedMessage() {
        UUID eventId = UUID.randomUUID();
        assertThat(processedRepository.existsByEventId(eventId)).isFalse();

        processedRepository.save(new ProcessedMessageEntity(eventId));

        assertThat(processedRepository.existsByEventId(eventId)).isTrue();
    }

}
