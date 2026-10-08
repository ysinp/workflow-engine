package com.example.kyc.service;

import com.example.kyc.client.IdentificationServiceFeignClient;
import com.example.kyc.dto.IdentificationView;
import com.example.kyc.exception.IdentificationRecordNotFoundException;
import com.example.kyc.exception.IdentificationServiceUnavailableException;
import feign.FeignException;
import org.springframework.stereotype.Component;

/** All I/O with identification-service lives here; the service layer never sees Feign. */
@Component
public class FeignIdentificationClient implements IdentificationClient {

    private final IdentificationServiceFeignClient client;

    public FeignIdentificationClient(IdentificationServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public IdentificationView get(Long identificationId) {
        try {
            return client.get(identificationId);
        } catch (FeignException.NotFound e) {
            throw new IdentificationRecordNotFoundException(identificationId);
        } catch (FeignException e) {
            throw new IdentificationServiceUnavailableException(
                    "identification-service lookup failed with HTTP " + e.status(), e);
        }
    }
}
