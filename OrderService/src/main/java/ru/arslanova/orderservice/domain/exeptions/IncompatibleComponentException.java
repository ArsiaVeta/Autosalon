package ru.arslanova.orderservice.domain.exeptions;

public class IncompatibleComponentException extends RuntimeException{
    public IncompatibleComponentException(String message){
        super(message);
    }
}
