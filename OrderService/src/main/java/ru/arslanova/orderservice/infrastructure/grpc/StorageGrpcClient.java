package ru.arslanova.orderservice.infrastructure.grpc;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.arslanova.grpc.car.*;
import ru.arslanova.orderservice.domain.exeptions.EntityNotFoundException;
import ru.arslanova.orderservice.domain.exeptions.StorageUnavailableException;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class StorageGrpcClient {

    private final CarAvailabilityServiceGrpc.CarAvailabilityServiceBlockingStub stub;
    private final long timeoutMs;

    private final long healthTimeoutMs;


    public StorageGrpcClient(
            CarAvailabilityServiceGrpc.CarAvailabilityServiceBlockingStub stub,
            @Value("${grpc.client.storage.timeout-ms:3000}") long timeoutMs,
            @Value("${grpc.client.storage.health-timeout-ms:1000}") long healthTimeoutMs) {
                this.stub = stub;
                this.timeoutMs = timeoutMs;
                this.healthTimeoutMs = healthTimeoutMs;
    }

    public int ping() {
        ListAvailableCarsResponse response = stub
                .withDeadlineAfter(healthTimeoutMs, TimeUnit.MILLISECONDS).
                listAvailableCars(ListAvailableCarsRequest.getDefaultInstance());
        return response.getCarsCount();
    }

    public List<AvailableCar> listAvailableCars() {
        log.info("gRPC -> StorageService.ListAvailableCars (timeout={}ms)", timeoutMs);
        try {
            ListAvailableCarsResponse response = stub
                    .withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS)
                    .listAvailableCars(ListAvailableCarsRequest.getDefaultInstance());
            log.info("gRPC <- StorageService.ListAvailableCars: {} car(s)", response.getCarsCount());
            return response.getCarsList();
        } catch (StatusRuntimeException e) {
            throw translate("ListAvailableCar", e);
        }
    }

    public AvailableCar getAvailableCar(String id) {
        log.info("gRPC -> StorageService.GetAvailableCar id={} (timeout={}ms)", id, timeoutMs);
        try {
            AvailableCar car = stub
                    .withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS)
                    .getAvailableCar(GetAvailableCarRequest.newBuilder().setId(id).build());
            log.info("gRPC <- StorageService.GetAvailableCar id={}: found", id);
            return car;
        } catch (StatusRuntimeException e) {
            throw translate("GetAvailableCar", e);
        }
    }

    private RuntimeException translate(String method, StatusRuntimeException e) {
        switch (e.getStatus().getCode()) {
            case NOT_FOUND:
                log.info("gRPC {} -> NOT_FOUND: {}", method, e.getStatus().getDescription());
                return new EntityNotFoundException(
                        e.getStatus().getDescription() != null
                                ? e.getStatus().getDescription()
                                : "Car not found");
            case INVALID_ARGUMENT:
                log.warn("gRPC {} -> INVALID_ARGUMENT: {}", method, e.getStatus().getDescription());
                return new EntityNotFoundException(
                        e.getStatus().getDescription() != null
                                ? e.getStatus().getDescription()
                                : "Invalid car id");
            default:
                log.error("gRPC {} failed: {} - {}", method,
                        e.getStatus().getCode(), e.getStatus().getDescription());
                return new StorageUnavailableException(
                        "StorageService is unavailable (" + e.getStatus().getCode() + ")", e);
        }
    }
}
