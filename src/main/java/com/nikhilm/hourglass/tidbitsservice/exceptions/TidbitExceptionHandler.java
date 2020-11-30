package com.nikhilm.hourglass.tidbitsservice.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class TidbitExceptionHandler {

    @ExceptionHandler(TidbitException.class)
    public ResponseEntity<ApiError> handleTidbitException(TidbitException e) {
        log.error("Exception " + e.getStatus() + " " + e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(new ApiError(String.valueOf(e.getStatus()), e.getMessage()));
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleGlobalException(RuntimeException e) {
        log.error("Runtime Exception " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("500", "Internal server error!"));
    }




}
