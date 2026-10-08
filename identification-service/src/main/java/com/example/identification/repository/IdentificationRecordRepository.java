package com.example.identification.repository;

import com.example.identification.entity.IdentificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentificationRecordRepository extends JpaRepository<IdentificationRecord, Long> {
}
