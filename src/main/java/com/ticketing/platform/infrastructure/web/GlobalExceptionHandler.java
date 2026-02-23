package com.ticketing.platform.infrastructure.web;

import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.exception.InsufficientInventoryException;
import com.ticketing.platform.domain.exception.NotFoundException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ApiError> handleConflict(InsufficientInventoryException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleValidation(WebExchangeBindException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
            .findFirst()
            .map(error -> {
                if (error instanceof FieldError fieldError) {
                    return fieldError.getField() + " " + fieldError.getDefaultMessage();
                }
                return error.getDefaultMessage();
            })
            .orElse("Invalid request");
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomainException(DomainException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
            new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message
            )
        );
    }
}
