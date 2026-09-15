package ru.arslanova.storageservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import ru.arslanova.storageservice.application.CarFilter;
import ru.arslanova.storageservice.config.JpaConfig;
import ru.arslanova.storageservice.domain.car.*;
import ru.arslanova.storageservice.infrastructure.entity.CarModelEntity;
import ru.arslanova.storageservice.infrastructure.repository.CarModelRepository;
import ru.arslanova.storageservice.infrastructure.repository.CarModelSpecifications;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false"
})
public class CarModelFilterIntegrationTest extends AbstractPostgresIntegrationTest{
    @Autowired
    CarModelRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        repository.save(car(Brand.BMW, "320i", new BigDecimal("3500000"), BodyType.SEDAN,
                FuelType.PETROL, DriveType.REAR, GearBox.AUTOMATIC, Color.BLACK, 2));
        repository.save(car(Brand.BMW, "330i", new BigDecimal("4500000"), BodyType.SEDAN,
                FuelType.PETROL, DriveType.ALL, GearBox.AUTOMATIC, Color.WHITE, 0));
        repository.save(car(Brand.MERCEDES, "GLE", new BigDecimal("7000000"), BodyType.SUV,
                FuelType.DIESEL, DriveType.ALL, GearBox.AUTOMATIC, Color.BLACK, 1));
    }

    @Test
    void filterByBrand() {
        CarFilter f = new CarFilter();
        f.setBrand(Brand.BMW);

        Page<CarModelEntity> result = repository.findAll(
                CarModelSpecifications.withFilter(f), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(c -> c.getBrand() == Brand.BMW);
    }


    @Test
    void filterByBrandAndModel() {
        CarFilter f = new CarFilter();
        f.setBrand(Brand.BMW);
        f.setModel("330i");

        Page<CarModelEntity> result = repository.findAll(
                CarModelSpecifications.withFilter(f), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getModel()).isEqualTo("330i");
    }

    @Test
    void filterByPriceRangeAndFuel() {
        CarFilter f = new CarFilter();
        f.setMinPrice(new BigDecimal("3000000"));
        f.setMaxPrice(new BigDecimal("5000000"));
        f.setFuelType(FuelType.PETROL);

        Page<CarModelEntity> result = repository.findAll(
                CarModelSpecifications.withFilter(f), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filterInStockOnly() {
        CarFilter f = new CarFilter();
        f.setInStockOnly(true);

        Page<CarModelEntity> result = repository.findAll(
                CarModelSpecifications.withFilter(f), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(c -> c.getStockCount() > 0);
    }

    @Test
    void filterByBodyAndDrive() {
        CarFilter f = new CarFilter();
        f.setBodyType(BodyType.SUV);
        f.setDriveType(DriveType.ALL);

        Page<CarModelEntity> result = repository.findAll(
                CarModelSpecifications.withFilter(f), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getBrand()).isEqualTo(Brand.MERCEDES);
    }

    @Test
    void filterByComponent() {
        UUID wheelId = UUID.randomUUID();
        UUID interiorId = UUID.randomUUID();

        CarModelEntity bmw = car(Brand.BMW, "M3", new BigDecimal("8000000"), BodyType.SEDAN,
                FuelType.PETROL, DriveType.REAR, GearBox.AUTOMATIC, Color.BLUE, 1);
        bmw.getBaseComponents().put(ComponentType.WHEEL, wheelId);
        bmw.getBaseComponents().put(ComponentType.INTERIOR, interiorId);
        repository.save(bmw);

        CarModelEntity audi = car(Brand.AUDI, "RS6", new BigDecimal("9500000"), BodyType.UNIVERSAL,
                FuelType.PETROL, DriveType.ALL, GearBox.AUTOMATIC, Color.BLACK, 1);
        audi.getBaseComponents().put(ComponentType.WHEEL, wheelId);
        repository.save(audi);

        Map<ComponentType, UUID> required = new EnumMap<>(ComponentType.class);
        required.put(ComponentType.WHEEL, wheelId);
        required.put(ComponentType.INTERIOR, interiorId);

        CarFilter f = new CarFilter();
        f.setComponents(required);

        Page<CarModelEntity> result = repository.findAll(
                CarModelSpecifications.withFilter(f), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getModel()).isEqualTo("M3");
    }

    @Test
    void emptyFilterReturnsAll() {
        Page<CarModelEntity> result = repository.findAll(
                CarModelSpecifications.withFilter(new CarFilter()), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    private CarModelEntity car(Brand brand, String model, BigDecimal price,
                               BodyType body, FuelType fuel, DriveType drive,
                               GearBox gearBox, Color color, int stock) {
        return new CarModelEntity(UUID.randomUUID(), brand, model, price,
                body, fuel, drive, gearBox, color, stock);
    }
}
