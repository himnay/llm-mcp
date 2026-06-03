package com.org.hr.repository;

import com.org.hr.model.LeaveRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface LeaveRecordRepository extends JpaRepository<LeaveRecord, Long> {
    Optional<LeaveRecord> findByUsernameAndLeaveDate(String username, LocalDate leaveDate);

}
