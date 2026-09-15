package ru.arslanova.orderservice.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.arslanova.orderservice.api.dto.CreateOrderRequest;
import ru.arslanova.orderservice.api.dto.OrderResponse;
import ru.arslanova.orderservice.api.mappers.OrderDtoMapper;
import ru.arslanova.orderservice.application.OrderService;
import ru.arslanova.orderservice.domain.Order.OrderContext;

import java.util.UUID;


@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Работа с заказами")
public class OrderController {
    private final OrderService service;
    private final OrderDtoMapper mapper;

    @PostMapping
    @Operation(summary = "Оформить заказ",
            description = "Создаёт заказ в статусе CREATED, автоматически назначает менеджера "
                    + "и для заказа с комплектацией отправляет его на согласование складу",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Заказ оформлен"),
                    @ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
                    @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")


            })
    public OrderResponse create(@RequestBody CreateOrderRequest request){
        OrderContext order = mapper.toDomain(request);
        OrderContext saved = service.create(order);
        return mapper.toResponse(saved);
    }

    @GetMapping
    @Operation(summary = "Получить список заказов",
    description = "Клиент видит только свои заказы, менеджер и администратор — все",
    responses = {
            @ApiResponse(responseCode = "200", description = "Список заказов получен успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public Page<OrderResponse> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return service.findAll(pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить заказ по идентификатору")
    public OrderResponse getById(@PathVariable UUID id) {return mapper.toResponse(service.findById(id)); }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Согласовать заказ менеджером",
            description = "CREATED -> APPROVED_BY_MANAGER. Доступно назначенному менеджеру и администратору. "
                    + "Заказ с комплектацией согласовывается складом автоматически")
    public OrderResponse approve(@PathVariable UUID id){
        return mapper.toResponse(service.approveByManager(id));
    }

    @PostMapping("/{id}/invoice")
    @Operation(summary = "Выставить счёт",
            description = "APPROVED_BY_MANAGER / APPROVED_BY_STORAGE -> AWAIT_FOR_PAYMENT")
    public OrderResponse invoice(@PathVariable UUID id){
        return mapper.toResponse(service.requestPayment(id));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Оплатить заказ",
            description = "AWAIT_FOR_PAYMENT -> PAID. Публикует событие для StorageService "
                    + "(резерв автомобиля в наличии или сборка комплектации)")
    public OrderResponse pay(@PathVariable UUID id){
        return mapper.toResponse(service.pay(id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Завершить заказ",
            description = "READY_FOR_PICKUP -> COMPLETED. Автомобиль выдан клиенту")
    public OrderResponse complete(@PathVariable UUID id){
        return mapper.toResponse(service.complete(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Отменить заказ",
            description = "Любой незавершённый статус -> CANCELLED")
    public OrderResponse cancel(@PathVariable UUID id){
        return mapper.toResponse(service.cancel(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить заказ")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
