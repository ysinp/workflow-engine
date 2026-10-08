package com.example.submission.service;

import com.example.submission.dto.AccountApplicationAcceptedResponse;
import com.example.submission.dto.AccountApplicationStartRequest;
import com.example.submission.exception.AccountApplicationServiceException;
import com.example.submission.exception.DuplicateApplicationException;

/**
 * DIP/SRP boundary: the only thing in this service that knows create-account's
 * application-staging endpoint is reached over HTTP. The service layer talks to this interface,
 * not to Feign directly.
 */
public interface AccountApplicationClient {

    /**
     * @throws DuplicateApplicationException      if this email already has an account, or a
     *                                             pending application
     * @throws AccountApplicationServiceException if create-account cannot be reached or responds
     *                                             unexpectedly
     */
    AccountApplicationAcceptedResponse submit(AccountApplicationStartRequest request);
}
