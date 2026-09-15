package ru.arslanova.orderservice.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.arslanova.orderservice.domain.TestDrive.TestDriveApplication;

import java.util.UUID;

public interface TestDriveRepository {
    TestDriveApplication save(TestDriveApplication application);
    TestDriveApplication findById(UUID id);
    Page<TestDriveApplication> findAll(Pageable pageable);

    Page<TestDriveApplication> findAllByClientId(UUID clientID, Pageable pageable);

    void deleteById(UUID id);
}
