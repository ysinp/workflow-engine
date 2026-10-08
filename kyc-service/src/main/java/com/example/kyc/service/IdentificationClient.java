package com.example.kyc.service;

import com.example.kyc.dto.IdentificationView;
import com.example.kyc.exception.IdentificationRecordNotFoundException;
import com.example.kyc.exception.IdentificationServiceUnavailableException;

/**
 * DIP/SRP boundary: the only thing in this service that knows identification-service is reached
 * over HTTP. {@link FinancialVerificationServiceImpl} talks to this interface, not to Feign directly.
 */
public interface IdentificationClient {

    /**
     * @throws IdentificationRecordNotFoundException      if no record exists with this id
     * @throws IdentificationServiceUnavailableException if the service cannot be reached or
     *                                                    responds unexpectedly
     */
    IdentificationView get(Long identificationId);
}
