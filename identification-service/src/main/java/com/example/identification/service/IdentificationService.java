package com.example.identification.service;

import com.example.identification.dto.DraftResponse;
import com.example.identification.dto.IdentificationView;
import com.example.identification.dto.SaveIdentificationRequest;
import com.example.identification.dto.UpdateIdentificationRequest;

public interface IdentificationService {

    /** Wizard step 1 (Personal + Contact). */
    DraftResponse save(SaveIdentificationRequest request);

    /**
     * Wizard step 2 (Identification) - a partial update (PATCH) attaching the document to an
     * already-created record.
     *
     * @throws com.example.identification.exception.IdentificationNotFoundException if no record exists with this id
     */
    DraftResponse updateIdentification(Long identificationId, UpdateIdentificationRequest request);

    /** @throws com.example.identification.exception.IdentificationNotFoundException if no record exists with this id */
    IdentificationView get(Long identificationId);
}
