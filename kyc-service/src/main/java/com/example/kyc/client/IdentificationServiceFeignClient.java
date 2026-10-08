package com.example.kyc.client;

import com.example.kyc.dto.IdentificationView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identification-service", url = "${services.identification-service.base-url}")
public interface IdentificationServiceFeignClient {

    @GetMapping("/api/identification/{identificationId}")
    IdentificationView get(@PathVariable("identificationId") Long identificationId);
}
