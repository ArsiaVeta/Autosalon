package ru.arslanova.orderservice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.arslanova.orderservice.domain.exeptions.DomainValidationExeption;
import ru.arslanova.orderservice.domain.repository.ManagerRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerAssignmentService {

    private final ManagerRepository managerRepository;

    public UUID assign(UUID orderId) {
        UUID managerId = managerRepository.findRandomManagerId()
                .orElseThrow(() -> new DomainValidationExeption(
                        "No manager available to handle the order"));

        log.info("MANAGER ASSIGNED orderId={} managerId={}", orderId, managerId);
        return managerId;
    }
}
