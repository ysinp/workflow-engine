package com.example.submission.service;

import com.example.submission.client.AccountApplicationFeignClient;
import com.example.submission.dto.AccountApplicationAcceptedResponse;
import com.example.submission.dto.AccountApplicationStartRequest;
import com.example.submission.exception.AccountApplicationServiceException;
import com.example.submission.exception.DuplicateApplicationException;
import feign.FeignException;
import org.springframework.stereotype.Component;

@Component
public class FeignAccountApplicationClient implements AccountApplicationClient {

    private final AccountApplicationFeignClient client;

    public FeignAccountApplicationClient(AccountApplicationFeignClient client) {
        this.client = client;
    }

    @Override
    public AccountApplicationAcceptedResponse submit(AccountApplicationStartRequest request) {
        try {
            return client.submit(request);
        } catch (FeignException.Conflict e) {
            throw new DuplicateApplicationException("An application for this email already exists");
        } catch (FeignException e) {
            throw new AccountApplicationServiceException(
                    "create-account application call failed with HTTP " + e.status(), e);
        }
    }
}
