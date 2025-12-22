package com.djy.quiz.exception;

import com.djy.quiz.response.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getFieldErrors()
        .stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(";"));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(msg));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Result<Void>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Result<Void>> handleGeneric(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Result.error("server error: " + ex.getMessage()));
  }
}
