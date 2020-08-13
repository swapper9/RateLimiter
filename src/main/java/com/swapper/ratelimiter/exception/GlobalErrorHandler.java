package com.swapper.ratelimiter.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Log4j2
public class GlobalErrorHandler {

    @SuppressWarnings("all")
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity processException(Exception e) {
        log.debug(e);
        return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
    }
}
