package br.com.organizadorfinanceiro.config;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                exception.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos", details));
    }

    @ExceptionHandler(IOException.class)
    ResponseEntity<ErrorResponse> storage(IOException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(
                Instant.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Não foi possível armazenar o arquivo com segurança", List.of()));
    }

    record ErrorResponse(Instant timestamp, int status, String message, List<String> details) {
    }
}
