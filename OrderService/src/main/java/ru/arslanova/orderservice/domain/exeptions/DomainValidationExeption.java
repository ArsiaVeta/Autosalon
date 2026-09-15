package ru.arslanova.orderservice.domain.exeptions;

public class DomainValidationExeption extends RuntimeException{
    public DomainValidationExeption(String message){
        super(message);
    }
}
