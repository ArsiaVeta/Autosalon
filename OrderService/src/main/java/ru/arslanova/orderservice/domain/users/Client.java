package ru.arslanova.orderservice.domain.users;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.arslanova.orderservice.domain.exeptions.DomainValidationExeption;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public class Client{
    private final UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    public final LocalDate dateOfBirth;
    public final String driverLicenseNumber;


    public Client(UUID id, String firstName, String lastName, String email, String phoneNumber, LocalDate dateOfBirth, String driverLicenseNumber) throws DomainValidationExeption {
        if (firstName == null){
            throw new DomainValidationExeption("First name cannot be empty");
        }

        if (lastName == null){
            throw new DomainValidationExeption("Last name cannot be empty");
        }

        if (dateOfBirth == null) {
            throw new DomainValidationExeption("Date of birth can not be null");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.driverLicenseNumber = driverLicenseNumber;
    }


    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public boolean isAdult() {
        return getAge() >= 18;
    }

}
