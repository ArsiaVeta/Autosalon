package ru.arslanova.orderservice.api.dto;

import java.time.Instant;
import java.util.List;

public record DependencyHealth(
        String name,
        String status,
        String detail
) {
    public static final String UP = "UP";
    public static final String DOWN = "DOWN";

    public static DependencyHealth up(String name, String detail) {
        return new DependencyHealth(name, UP, detail);
    }

    public static DependencyHealth down(String name, String detail) {
        return new DependencyHealth(name, DOWN, detail);
    }

    public boolean isUp(){
        return UP.equals(status);
    }
}
