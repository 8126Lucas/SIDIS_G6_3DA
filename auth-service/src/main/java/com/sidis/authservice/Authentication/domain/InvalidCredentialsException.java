package com.sidis.authservice.Authentication.domain;

import com.sidis.authservice.exceptions.DomainException;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
