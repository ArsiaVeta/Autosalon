package ru.arslanova.storageservice.infrastructure.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.arslanova.grpc.car.*;
import ru.arslanova.storageservice.infrastructure.entity.CarModelEntity;
import ru.arslanova.storageservice.infrastructure.repository.CarModelRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarAvailabilityGrpcService
        extends CarAvailabilityServiceGrpc.CarAvailabilityServiceImplBase {

    private final CarModelRepository carModelRepository;

    @Override
    public void listAvailableCars(ListAvailableCarsRequest request,
                                  StreamObserver<ListAvailableCarsResponse> responseObserver) {
        log.info("gRPC <- ListAvailableCars");
        List<CarModelEntity> cars = carModelRepository.findByStockCountGreaterThanAndRemovedFalse(0);

        ListAvailableCarsResponse.Builder response = ListAvailableCarsResponse.newBuilder();
        for (CarModelEntity car : cars) {
            response.addCars(toProto(car));
        }
        log.info("gRPC -> ListAvailableCars: returned {} available car(s)", cars.size());

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAvailableCar(GetAvailableCarRequest request,
                                StreamObserver<AvailableCar> responseObserver) {
        String rawId = request.getId();
        log.info("gRPC <- GetAvailableCar id={}", rawId);

        UUID id;
        try {
            id = UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("Invalid car id: " + rawId)
                    .asRuntimeException());
            return;
        }

        CarModelEntity car = carModelRepository.findById(id).orElse(null);
        if (car == null || car.isRemoved() || car.getStockCount() <= 0) {
            log.info("gRPC -> GetAvailableCar id={}: NOT_FOUND (not available)", rawId);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Available car not found: " + rawId)
                    .asRuntimeException());
            return;
        }

        log.info("gRPC -> GetAvailableCar id={}: found (stock={})", rawId, car.getStockCount());
        responseObserver.onNext(toProto(car));
        responseObserver.onCompleted();
    }

    private AvailableCar toProto(CarModelEntity e) {
        AvailableCar.Builder b = AvailableCar.newBuilder()
                .setId(e.getId().toString())
                .setModel(nullSafe(e.getModel()))
                .setStockCount(e.getStockCount());
        if (e.getBrand() != null) b.setBrand(e.getBrand().name());
        if (e.getBasePrice() != null) b.setBasePrice(e.getBasePrice().toPlainString());
        if (e.getBodyType() != null) b.setBodyType(e.getBodyType().name());
        if (e.getFuelType() != null) b.setFuelType(e.getFuelType().name());
        if (e.getDriveType() != null) b.setDriveType(e.getDriveType().name());
        if (e.getGearBox() != null) b.setGearBox(e.getGearBox().name());
        if (e.getColor() != null) b.setColor(e.getColor().name());
        return b.build();
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
