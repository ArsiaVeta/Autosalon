package ru.arslanova.storageservice.api.errors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.arslanova.storageservice.domain.exeptions.DomainValidationExeption;
import ru.arslanova.storageservice.domain.exeptions.EntityNotFoundException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(
            EntityNotFoundException exception,
            HttpServletRequest request){
        return new ErrorResponse(
                Instant.now(), 404, "NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DomainValidationExeption.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(
            DomainValidationExeption exception,
            HttpServletRequest request){
        return new ErrorResponse(
                Instant.now(), 400, "BAD_REQUEST",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException e, HttpServletRequest req){
        return new ErrorResponse(
                Instant.now(), 403, "FORBIDDEN",
                e.getMessage() != null ? e.getMessage() : "Access denied",
                req.getRequestURI()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuth(AuthenticationException e, HttpServletRequest req){
        return new ErrorResponse(
                Instant.now(), 401, "UNAUTHORIZED",
                e.getMessage() != null ? e.getMessage() : "Authentication required",
                req.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request){
        return new ErrorResponse(
                Instant.now(), 400, "BAD_REQUEST",
                "Malformed request body: " + rootMessage(exception),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request){
        return new ErrorResponse(
                Instant.now(), 400, "BAD_REQUEST",
                "Invalid value for parameter '" + exception.getName() + "': " + exception.getValue(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request){
        return new ErrorResponse(
                Instant.now(), 400, "BAD_REQUEST",
                exception.getMessage() != null ? exception.getMessage() : "Invalid request",
                request.getRequestURI()
        );
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnknown(
            Exception exception,
            HttpServletRequest request){

        log.error("Unhandled exception on {}", request.getRequestURI(), exception);

        return new ErrorResponse(
                Instant.now(), 500, "INTERNAL_SERVER_ERROR",
                "Unexpected server error",
                request.getRequestURI()
        );
    }

    private String rootMessage(Throwable exception){
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : exception.getMessage();
    }

}
