package com.example.identification.service;

import com.example.identification.dto.DraftResponse;
import com.example.identification.dto.IdentificationView;
import com.example.identification.dto.SaveIdentificationRequest;
import com.example.identification.dto.UpdateIdentificationRequest;
import com.example.identification.dto.WizardStep;
import com.example.identification.entity.IdentificationRecord;
import com.example.identification.exception.IdentificationNotFoundException;
import com.example.identification.repository.IdentificationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentificationServiceImpl implements IdentificationService {

    private final IdentificationRecordRepository repository;

    public IdentificationServiceImpl(IdentificationRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DraftResponse save(SaveIdentificationRequest request) {
        IdentificationRecord record = request.identificationId() != null
                ? repository.findById(request.identificationId()).orElse(null)
                : null;

        if (record == null) {
            record = new IdentificationRecord(
                    request.fullName(), request.dateOfBirth(), request.nationality(), request.gender(),
                    request.email(), request.phone(), request.addressLine1(), request.addressLine2(),
                    request.city(), request.state(), request.postalCode(), request.country());
        } else {
            // The wizard went back and re-submitted this step - update in place rather than
            // creating an orphaned duplicate row. Never touches idType/idNumber (see
            // updateIdentification) even if they were already set by a later step.
            record.updatePersonalContact(
                    request.fullName(), request.dateOfBirth(), request.nationality(), request.gender(),
                    request.email(), request.phone(), request.addressLine1(), request.addressLine2(),
                    request.city(), request.state(), request.postalCode(), request.country());
        }

        IdentificationRecord saved = repository.save(record);
        return new DraftResponse(saved.getId(), WizardStep.IDENTIFICATION);
    }

    @Override
    @Transactional
    public DraftResponse updateIdentification(Long identificationId, UpdateIdentificationRequest request) {
        IdentificationRecord record = repository.findById(identificationId)
                .orElseThrow(() -> new IdentificationNotFoundException(identificationId));

        record.updateIdentification(request.idType(), request.idNumber());
        IdentificationRecord saved = repository.save(record);
        return new DraftResponse(saved.getId(), WizardStep.FINANCIAL);
    }

    @Override
    public IdentificationView get(Long identificationId) {
        return repository.findById(identificationId)
                .map(IdentificationView::from)
                .orElseThrow(() -> new IdentificationNotFoundException(identificationId));
    }
}
