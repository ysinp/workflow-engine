package com.example.submission.client;

import com.example.submission.dto.AccountApplicationAcceptedResponse;
import com.example.submission.dto.AccountApplicationStartRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Declarative client for create-account's application-staging endpoint. */
@FeignClient(name = "create-account", url = "${services.create-account.base-url}")
public interface AccountApplicationFeignClient {

    @PostMapping("/api/account-applications")
    AccountApplicationAcceptedResponse submit(@RequestBody AccountApplicationStartRequest request);
}
