package ru.arslanova.storageservice.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.arslanova.storageservice.api.dto.DependencyHealth;
import ru.arslanova.storageservice.api.dto.HealthResponse;
import ru.arslanova.storageservice.infrastructure.config.TraceIdFilter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Состояние сервиса ни его зависимостей")
public class HealthController {
    private static final String SERVICE_NAME = "storage-service";
    private static final String DEP_DATABASE = "database";
    private static final int DB_VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    @GetMapping
    @Operation(summary = "Состояние сервиса",
        description = "Возвращает статус StorageService, доступность собственной БД "
    + "Аутентификация не требуется.",
    responses = {
            @ApiResponse(responseCode = "200", description = "Сервис и все зависимости доступны"),
            @ApiResponse(responseCode = "503", description = "Недоступна хотя бы одна ключевая зависимость")
    })

    public ResponseEntity<HealthResponse> health() {
        List<DependencyHealth> dependencies = List.of(checkDatabase());

        HealthResponse body = HealthResponse.of(SERVICE_NAME, currentTraceId(), dependencies);

        if (!body.isUp()){
            log.warn("HEALTH CHECK DOWN service={} dependencies={}", SERVICE_NAME, dependencies);
        }

        return ResponseEntity
                .status(body.isUp() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }

    private DependencyHealth checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(DB_VALIDATION_TIMEOUT_SECONDS)) {
                return DependencyHealth.up(DEP_DATABASE,
                        connection.getMetaData().getDatabaseProductName());
            }
            return DependencyHealth.down(DEP_DATABASE, "Connection is not valid");
        } catch (Exception e) {
            log.warn("HEALTH CHECK: database is unanailable: {}", e.getMessage());
            return DependencyHealth.down(DEP_DATABASE, e.getMessage());
        }
    }
    private String currentTraceId() {
        return MDC.get(TraceIdFilter.MDC_KEY);
    }

}
