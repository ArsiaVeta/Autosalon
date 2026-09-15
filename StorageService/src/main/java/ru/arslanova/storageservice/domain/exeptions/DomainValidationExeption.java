package ru.arslanova.storageservice.domain.exeptions;

public class DomainValidationExeption extends RuntimeException{
    public DomainValidationExeption(String message){
        super(message);
    }
}
