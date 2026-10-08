package com.example.identification.service;

import com.example.identification.dto.DraftResponse;
import com.example.identification.dto.IdentificationView;
import com.example.identification.dto.SaveIdentificationRequest;
import com.example.identification.dto.UpdateIdentificationRequest;
import com.example.identification.entity.IdentificationRecord;
import com.example.identification.exception.IdentificationNotFoundException;
import com.example.identification.repository.IdentificationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentificationServiceImplTest {

    @Mock
    private IdentificationRecordRepository repository;

    private IdentificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new IdentificationServiceImpl(repository);
    }

    @Test
    void save_withoutId_createsNewRecord() {
        SaveIdentificationRequest request = personalContactRequest(null);
        when(repository.save(any(IdentificationRecord.class))).thenAnswer(inv -> {
            IdentificationRecord record = inv.getArgument(0);
            setId(record, 42L);
            return record;
        });

        DraftResponse response = service.save(request);

        assertThat(response.identificationId()).isEqualTo(42L);
        assertThat(response.nextStep()).isEqualTo("identification");
        verify(repository, never()).findById(anyLong());
    }

    @Test
    void save_withExistingId_updatesPersonalContactInPlaceWithoutTouchingIdentification() {
        IdentificationRecord existing = new IdentificationRecord(
                "Old Name", "1990-01-01", "US", "", "old@example.com", "", "1 Old St", "",
                "Oldtown", "", "", "US");
        existing.updateIdentification("PASSPORT", "P9999999"); // already attached by a later step
        setId(existing, 7L);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any(IdentificationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        DraftResponse response = service.save(personalContactRequest(7L));

        assertThat(response.identificationId()).isEqualTo(7L);
        assertThat(response.nextStep()).isEqualTo("identification");
        ArgumentCaptor<IdentificationRecord> captor = ArgumentCaptor.forClass(IdentificationRecord.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getFullName()).isEqualTo("Jane Doe");
        assertThat(captor.getValue().getEmail()).isEqualTo("jane@example.com");
        // identification fields from the earlier step are untouched by this personal/contact update
        assertThat(captor.getValue().getIdType()).isEqualTo("PASSPORT");
        assertThat(captor.getValue().getIdNumber()).isEqualTo("P9999999");
    }

    @Test
    void updateIdentification_attachesDocumentAndReturnsFinancialAsNextStep() {
        IdentificationRecord existing = new IdentificationRecord(
                "Jane Doe", "1990-01-01", "US", "", "jane@example.com", "", "1 Main St", "",
                "Metropolis", "", "", "US");
        setId(existing, 9L);
        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        when(repository.save(any(IdentificationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        DraftResponse response = service.updateIdentification(9L, new UpdateIdentificationRequest("PASSPORT", "P1234567"));

        assertThat(response.identificationId()).isEqualTo(9L);
        assertThat(response.nextStep()).isEqualTo("financial");
        assertThat(existing.getIdType()).isEqualTo("PASSPORT");
        assertThat(existing.getIdNumber()).isEqualTo("P1234567");
    }

    @Test
    void updateIdentification_throwsWhenRecordDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateIdentification(404L, new UpdateIdentificationRequest("PASSPORT", "P1")))
                .isInstanceOf(IdentificationNotFoundException.class);
    }

    @Test
    void get_returnsViewForExistingRecord() {
        IdentificationRecord record = new IdentificationRecord(
                "Jane Doe", "1990-01-01", "US", "", "jane@example.com", "", "1 Main St", "",
                "Metropolis", "", "", "US");
        record.updateIdentification("PASSPORT", "P1234567");
        setId(record, 9L);
        when(repository.findById(9L)).thenReturn(Optional.of(record));

        IdentificationView view = service.get(9L);

        assertThat(view.identificationId()).isEqualTo(9L);
        assertThat(view.idType()).isEqualTo("PASSPORT");
        assertThat(view.idNumber()).isEqualTo("P1234567");
    }

    @Test
    void get_throwsWhenRecordDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(404L)).isInstanceOf(IdentificationNotFoundException.class);
    }

    private SaveIdentificationRequest personalContactRequest(Long identificationId) {
        return new SaveIdentificationRequest(identificationId, "Jane Doe", "1990-01-01", "US", "",
                "jane@example.com", "", "1 Main St", "", "Metropolis", "", "", "US");
    }

    private void setId(IdentificationRecord record, Long id) {
        try {
            var field = IdentificationRecord.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(record, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
