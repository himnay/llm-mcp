package com.nexacorp.hr.service;

import com.nexacorp.hr.model.Employee;
import com.nexacorp.hr.model.LeaveRecord;
import com.nexacorp.hr.model.LeaveStatus;
import com.nexacorp.hr.repository.EmployeeRepository;
import com.nexacorp.hr.repository.LeaveRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HRService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRecordRepository leaveRecordRepository;

    public LeaveRecord applyLeave(String username, LocalDate date) {
        return leaveRecordRepository
                .findByUsernameAndLeaveDate(username, date)
                .orElseGet(() -> {
                    LeaveRecord leave = LeaveRecord.builder()
                            .username(username)
                            .leaveDate(date)
                            .status(LeaveStatus.APPROVED)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return leaveRecordRepository.save(leave);
                });
    }

    public boolean isOnLeave(String username, LocalDate date) {
        return leaveRecordRepository
                .findByUsernameAndLeaveDate(username, date)
                .isPresent();
    }

    public String findReplacement(String username, LocalDate date) {
        Employee current = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        List<Employee> teamMembers = employeeRepository.findByTeam(current.getTeam());
        for (Employee member : teamMembers) {
            if (member.getUsername().equals(username)) {
                continue;
            }
            boolean onLeave = leaveRecordRepository
                    .findByUsernameAndLeaveDate(member.getUsername(), date)
                    .isPresent();
            if (!onLeave) {
                return member.getUsername();
            }
        }
        throw new RuntimeException("No replacement available");
    }


}
