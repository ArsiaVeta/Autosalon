package ru.arslanova.storageservice.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.arslanova.storageservice.domain.exeptions.CarNotAvailableException;
import ru.arslanova.storageservice.infrastructure.entity.CarModelEntity;
import ru.arslanova.storageservice.infrastructure.repository.CarModelRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarAvailabilityService {
    private final CarModelRepository carModelRepository;

    public void ensureAvailable(UUID carModelId, String orderType){
        if (carModelId == null){
            throw new CarNotAvailableException("carModelId is null");
        }
        CarModelEntity car = carModelRepository.findById(carModelId)
                .orElseThrow(() -> new CarNotAvailableException(
                        "Car model not found: " + carModelId));

        if ("IN_STOCK".equalsIgnoreCase(orderType)){
            if (car.getStockCount() <= 0){
                throw new CarNotAvailableException(
                        "Car not in stock: " + carModelId);
            }
        }
        log.info("Car availability OK: carModelId={} orderType={} stock={}",
                carModelId, orderType, car.getStockCount());
    }
}
