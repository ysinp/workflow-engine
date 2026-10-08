package com.example.identification.controller;

import com.example.identification.dto.DraftResponse;
import com.example.identification.dto.IdentificationView;
import com.example.identification.dto.SaveIdentificationRequest;
import com.example.identification.dto.UpdateIdentificationRequest;
import com.example.identification.service.IdentificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identification")
public class IdentificationController {

    private final IdentificationService identificationService;

    public IdentificationController(IdentificationService identificationService) {
        this.identificationService = identificationService;
    }

    /** Wizard step 1 (Personal + Contact). */
    @PostMapping
    public DraftResponse save(@Valid @RequestBody SaveIdentificationRequest request) {
        return identificationService.save(request);
    }

    /** Wizard step 2 (Identification) - a partial update of the record step 1 created. */
    @PatchMapping("/{identificationId}")
    public DraftResponse updateIdentification(@PathVariable Long identificationId,
                                               @Valid @RequestBody UpdateIdentificationRequest request) {
        return identificationService.updateIdentification(identificationId, request);
    }

    /** Read-only lookup kyc-service calls (via Feign) to verify the Financial step against. */
    @GetMapping("/{identificationId}")
    public IdentificationView get(@PathVariable Long identificationId) {
        return identificationService.get(identificationId);
    }
}
