package ru.arslanova.orderservice.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "clients")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ClientEntity extends BaseEntity{

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    private String driverLicenseNumber;

    public ClientEntity(UUID id, String firstName, String lastName,
                        String email, String phoneNumber,
                        LocalDate dateOfBirth, String driverLicenseNumber){
        this.setId(id);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.driverLicenseNumber = driverLicenseNumber;

    }

}
