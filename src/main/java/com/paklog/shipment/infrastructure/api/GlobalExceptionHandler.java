package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.exception.ShipmentCreationException;
import com.paklog.shipment.application.exception.ShipmentNotFoundException;
import com.paklog.shipment.domain.exception.CarrierException;
import com.paklog.shipment.infrastructure.api.dto.ErrorResponse;
import com.paklog.shipment.infrastructure.api.dto.FieldErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        List<FieldErrorResponse> fieldErrors = bindingResult.getFieldErrors().stream()
            .map(fieldError -> new FieldErrorResponse(fieldError.getField(), resolveMessage(fieldError)))
            .collect(Collectors.toList());
        ErrorResponse response = new ErrorResponse("validation_error", "Validation failed", Instant.now(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ShipmentNotFoundException ex) {
        ErrorResponse response = new ErrorResponse("shipment_not_found", ex.getMessage(), Instant.now(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({ShipmentCreationException.class, CarrierException.class})
    public ResponseEntity<ErrorResponse> handleCarrierErrors(RuntimeException ex) {
        ErrorResponse response = new ErrorResponse("shipment_creation_failed", ex.getMessage(), Instant.now(), null);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse response = new ErrorResponse("internal_error", "An unexpected error occurred", Instant.now(), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String resolveMessage(FieldError fieldError) {
        if (fieldError.getDefaultMessage() != null) {
            return fieldError.getDefaultMessage();
        }
        return "Invalid value";
    }
}
