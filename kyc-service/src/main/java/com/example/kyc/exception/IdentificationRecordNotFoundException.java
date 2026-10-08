package com.example.kyc.exception;

/** No identification-service record exists for the {@code identificationId} the wizard sent. */
public class IdentificationRecordNotFoundException extends RuntimeException {

    public IdentificationRecordNotFoundException(Long identificationId) {
        super("No identification record found with id " + identificationId
                + " - complete the Personal/Contact/Identification step first");
    }
}
