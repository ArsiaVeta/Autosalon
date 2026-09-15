package ru.arslanova.orderservice.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.arslanova.orderservice.api.dto.ClientResponse;
import ru.arslanova.orderservice.api.dto.CreateClientRequest;
import ru.arslanova.orderservice.domain.users.Client;

@Mapper(componentModel = "spring")
public interface ClientDtoMapper {
    @Mapping(target = "id", ignore = true)
    Client toDomain(CreateClientRequest dto);

    ClientResponse toResponse(Client client);

}
