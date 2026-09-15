package ru.arslanova.storageservice.infrastructure.repository;

import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import ru.arslanova.storageservice.application.CarFilter;
import ru.arslanova.storageservice.domain.car.ComponentType;
import ru.arslanova.storageservice.infrastructure.entity.CarModelEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CarModelSpecifications {

    private CarModelSpecifications() {}

    public static Specification<CarModelEntity> withFilter(CarFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("removed")));

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (filter.getBrand() != null) {
                predicates.add(cb.equal(root.get("brand"), filter.getBrand()));
            }
            if (filter.getModel() != null && !filter.getModel().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("model")), filter.getModel().toLowerCase()));
            }
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), filter.getMaxPrice()));
            }
            if (filter.getBodyType() != null) {
                predicates.add(cb.equal(root.get("bodyType"), filter.getBodyType()));
            }
            if (filter.getFuelType() != null) {
                predicates.add(cb.equal(root.get("fuelType"), filter.getFuelType()));
            }
            if (filter.getGearBox() != null) {
                predicates.add(cb.equal(root.get("gearBox"), filter.getGearBox()));
            }
            if (filter.getDriveType() != null) {
                predicates.add(cb.equal(root.get("driveType"), filter.getDriveType()));
            }
            if (filter.getColor() != null) {
                predicates.add(cb.equal(root.get("color"), filter.getColor()));
            }
            if (Boolean.TRUE.equals(filter.getInStockOnly())) {
                predicates.add(cb.greaterThan(root.get("stockCount"), 0));
            }

            if (filter.getComponents() != null && !filter.getComponents().isEmpty()) {
                if (query != null) {
                    query.distinct(true);
                }
                for (Map.Entry<ComponentType, UUID> entry : filter.getComponents().entrySet()) {
                    Subquery<UUID> sub = query == null ? null : query.subquery(UUID.class);
                    if (sub != null) {
                        var subRoot = sub.from(CarModelEntity.class);
                        MapJoin<CarModelEntity, ComponentType, UUID> join =
                                subRoot.joinMap("baseComponents");
                        sub.select(subRoot.get("id"))
                                .where(
                                        cb.equal(subRoot.get("id"), root.get("id")),
                                        cb.equal(join.key(), entry.getKey()),
                                        cb.equal(join.value(), entry.getValue())
                                );
                        predicates.add(cb.exists(sub));
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
