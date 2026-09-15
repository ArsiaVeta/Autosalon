package ru.arslanova.storageservice.domain.exeptions;

public class IncompatibleComponentException extends RuntimeException{
    public IncompatibleComponentException(String message){
        super(message);
    }
}
