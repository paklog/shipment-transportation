package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.exception.LoadNotFoundException;
import com.paklog.shipment.application.exception.ShipmentNotFoundException;
import com.paklog.shipment.infrastructure.api.gen.dto.Problem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining(", "));

        return buildResponse(HttpStatus.BAD_REQUEST, URI.create("/problems/validation-error"), "One or more fields have an error: " + detail);
    }

    @ExceptionHandler(LoadNotFoundException.class)
    public ResponseEntity<Problem> handleLoadNotFoundException(LoadNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, URI.create("/problems/load-not-found"), ex.getMessage());
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<Problem> handleShipmentNotFoundException(ShipmentNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, URI.create("/problems/shipment-not-found"), ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Problem> handleIllegalStateException(IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, URI.create("/problems/conflict"), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleGenericException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, URI.create("/problems/internal-server-error"), "An unexpected error occurred: " + ex.getMessage());
    }

    private ResponseEntity<Problem> buildResponse(HttpStatus status, URI type, String detail) {
        Problem problem = new Problem();
        problem.setType(type);
        problem.setTitle(status.getReasonPhrase());
        problem.setStatus(status.value());
        problem.setDetail(detail);
        return new ResponseEntity<>(problem, status);
    }
}
