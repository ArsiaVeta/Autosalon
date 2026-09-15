package ru.arslanova.orderservice.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.arslanova.orderservice.domain.TestDrive.TestDriveApplication;
import ru.arslanova.orderservice.domain.exeptions.EntityNotFoundException;
import ru.arslanova.orderservice.domain.repository.TestDriveRepository;
import ru.arslanova.orderservice.infrastructure.mappers.TestDrivePersistenceMapper;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaTestDriveRepositoryAdapter implements TestDriveRepository {

    private final JpaTestDriveRepository jpaRepository;
    private final TestDrivePersistenceMapper mapper;

    @Override
    public TestDriveApplication save(TestDriveApplication application) {
        var entity = mapper.toEntity(application);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public TestDriveApplication findById(UUID id){
        return jpaRepository.findById(id)
                .map(mapper::toDomain)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));

    }

    @Override
    public Page<TestDriveApplication> findAll(Pageable pageable){
        return jpaRepository.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<TestDriveApplication> findAllByClientId(UUID clientId, Pageable pageable){
        return jpaRepository.findAllByClientId(clientId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id){
        jpaRepository.deleteById(id);
    }
}
