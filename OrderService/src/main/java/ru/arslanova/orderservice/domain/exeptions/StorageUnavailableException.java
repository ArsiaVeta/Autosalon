package ru.arslanova.orderservice.domain.exeptions;

public class StorageUnavailableException extends RuntimeException {
    public StorageUnavailableException(String message, Throwable cause){super(message, cause); }
}
