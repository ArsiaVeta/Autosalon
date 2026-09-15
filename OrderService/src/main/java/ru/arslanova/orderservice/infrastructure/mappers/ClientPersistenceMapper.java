package ru.arslanova.orderservice.infrastructure.mappers;

import org.springframework.stereotype.Component;
import ru.arslanova.orderservice.domain.users.Client;
import ru.arslanova.orderservice.infrastructure.entity.ClientEntity;


@Component
public class ClientPersistenceMapper {
    public ClientEntity toEntity(Client client){
        return new ClientEntity(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail(),
                client.getPhoneNumber(),
                client.getDateOfBirth(),
                client.getDriverLicenseNumber()
        );
    }

    public Client toDomain(ClientEntity entity){
        return new Client(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getDateOfBirth(),
                entity.getDriverLicenseNumber()
        );
    }

}
