package ru.arslanova.orderservice.api.dto;

import ru.arslanova.grpc.car.AvailableCar;

import java.math.BigDecimal;

public record CarResponse (
    String id,
    String brand,
    String model,
    BigDecimal basePrice,
    String bodyType,
    String fuelType,
    String driveType,
    String gearBox,
    String color,
    int stockCount
) {
    public static CarResponse from(AvailableCar c) {
        BigDecimal price = c.getBasePrice() == null || c.getBasePrice().isBlank()
                ? null
                : new BigDecimal(c.getBasePrice());
        return new CarResponse(
                c.getId(),
                emptyToNull(c.getBrand()),
                emptyToNull(c.getModel()),
                price,
                emptyToNull(c.getBodyType()),
                emptyToNull(c.getFuelType()),
                emptyToNull(c.getDriveType()),
                emptyToNull(c.getGearBox()),
                emptyToNull(c.getColor()),
                c.getStockCount()
        );
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}