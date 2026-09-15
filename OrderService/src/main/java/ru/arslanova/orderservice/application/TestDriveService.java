package ru.arslanova.orderservice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.arslanova.orderservice.domain.TestDrive.TestDriveApplication;
import ru.arslanova.orderservice.domain.repository.TestDriveRepository;
import ru.arslanova.orderservice.infrastructure.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TestDriveService {
    private final TestDriveRepository repository;

    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public TestDriveApplication create(UUID clientId, UUID carId, LocalDateTime time){
        UUID effectiveClientId = clientId;
        try{
            UUID currentUserId = SecurityUtils.getCurrentUserId();
            if (effectiveClientId == null){
                effectiveClientId = currentUserId;
            }
        } catch (Exception ignored){
        }
        return repository.save(new TestDriveApplication(effectiveClientId, carId, time));
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Page<TestDriveApplication> findAll(Pageable pageable){
        return repository.findAll(pageable);
    }

    public Page<TestDriveApplication> findMine(Pageable pageable){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManagerOrAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        if (isManagerOrAdmin) {
            return repository.findAll(pageable);
        }
        return repository.findAllByClientId(SecurityUtils.getCurrentUserId(), pageable);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void delete(UUID id) {
        repository.deleteById(id);
    }

}
