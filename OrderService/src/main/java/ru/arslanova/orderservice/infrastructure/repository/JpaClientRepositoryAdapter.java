package ru.arslanova.orderservice.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.arslanova.orderservice.domain.exeptions.EntityNotFoundException;
import ru.arslanova.orderservice.domain.repository.ClientRepository;
import ru.arslanova.orderservice.domain.users.Client;
import ru.arslanova.orderservice.infrastructure.mappers.ClientPersistenceMapper;


import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaClientRepositoryAdapter implements ClientRepository {
    private final JpaClientRepository jpaRepository;
    private final ClientPersistenceMapper mapper;

    @Override
    public Client save(Client client){
        var entity = mapper.toEntity(client);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    @Override
    public Client findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));
    }

    public Page<Client> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id){
        jpaRepository.deleteById(id);
    }

}
