package com.cinezone.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BenefitMonthlyLimitExceededException extends RuntimeException {
    public BenefitMonthlyLimitExceededException(String message) {
        super(message);
    }
}
