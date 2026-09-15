package ru.arslanova.orderservice.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.arslanova.orderservice.api.dto.ClientResponse;
import ru.arslanova.orderservice.api.dto.CreateClientRequest;
import ru.arslanova.orderservice.api.mappers.ClientDtoMapper;
import ru.arslanova.orderservice.application.ClientService;
import ru.arslanova.orderservice.domain.users.Client;

@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RestController
@RequestMapping("api/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Работа с клиентами")
public class ClientController{
    private final ClientService clientService;
    private final ClientDtoMapper mapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Создать клиента",
            description = "Создаёт нового клиента и сохраненяет его в БД",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Клиент успешно сохранён"),
                    @ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
            })
    public ClientResponse create(@RequestBody CreateClientRequest request){
        Client client = mapper.toDomain(request);
        Client saved = clientService.create(client);
        return mapper.toResponse(saved);
    }

}

