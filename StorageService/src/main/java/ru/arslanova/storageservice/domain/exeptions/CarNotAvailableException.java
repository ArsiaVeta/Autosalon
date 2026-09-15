package ru.arslanova.storageservice.domain.exeptions;

public class CarNotAvailableException extends RuntimeException{
    public CarNotAvailableException(String message) {
        super(message);
    }
}
