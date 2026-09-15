package ru.arslanova.orderservice.api.dto;

import java.time.Instant;
import java.util.List;

public record HealthResponse (
    String status,
    String service,
    Instant timestamp,
    String traceId,
    List<DependencyHealth> dependencies
){
    public static final String UP = "UP";
    public static final String DOWN = "DOWN";

    public static HealthResponse of(String service,
                                    String traceId,
                                    List<DependencyHealth> dependencies){

        boolean allUp = dependencies.stream().allMatch(DependencyHealth::isUp);
        return new HealthResponse(
                allUp ? UP : DOWN,
                service,
                Instant.now(),
                traceId,
                dependencies
        );
    }
    public boolean isUp() {
        return UP.equals(status);
    }
}
