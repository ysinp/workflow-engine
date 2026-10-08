package com.example.identification.exception;

public class IdentificationNotFoundException extends RuntimeException {

    public IdentificationNotFoundException(Long identificationId) {
        super("No identification record found with id " + identificationId);
    }
}
