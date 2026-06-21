package com.org.hr.service;

import com.org.hr.TestcontainersConfiguration;
import com.org.hr.exception.ResourceNotFoundException;
import com.org.hr.model.Employee;
import com.org.hr.model.LeaveRecord;
import com.org.hr.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class HRServiceIntegrationTest {

    @Autowired
    private HRService hrService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void cleanup() {
        employeeRepository.deleteAll();
    }

    private Employee saveEmployee(String username, String fullName, String team) {
        return employeeRepository.save(Employee.builder()
                .username(username)
                .fullName(fullName)
                .team(team)
                .build());
    }

    @DisplayName("Creates a new leave record with the correct username and date")
    @Test
    void applyLeave_createsNewLeaveRecord() {
        saveEmployee("alice", "Alice Smith", "backend");
        LocalDate leaveDate = LocalDate.of(2026, 7, 10);

        LeaveRecord record = hrService.applyLeave("alice", leaveDate);

        assertThat(record.getId()).isNotNull();
        assertThat(record.getUsername()).isEqualTo("alice");
        assertThat(record.getLeaveDate()).isEqualTo(leaveDate);
    }

    @DisplayName("Returns the same leave record when applying leave twice for the same date")
    @Test
    void applyLeave_idempotent_returnsSameRecord() {
        saveEmployee("bob", "Bob Jones", "frontend");
        LocalDate leaveDate = LocalDate.of(2026, 7, 15);

        LeaveRecord first = hrService.applyLeave("bob", leaveDate);
        LeaveRecord second = hrService.applyLeave("bob", leaveDate);

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @DisplayName("isOnLeave returns true when a leave record exists for that date")
    @Test
    void isOnLeave_returnsTrue_whenLeaveExists() {
        saveEmployee("carol", "Carol White", "devops");
        LocalDate leaveDate = LocalDate.of(2026, 8, 1);
        hrService.applyLeave("carol", leaveDate);

        assertThat(hrService.isOnLeave("carol", leaveDate)).isTrue();
    }

    @DisplayName("isOnLeave returns false when no leave record exists for that date")
    @Test
    void isOnLeave_returnsFalse_whenNoLeave() {
        saveEmployee("dave", "Dave Brown", "qa");

        assertThat(hrService.isOnLeave("dave", LocalDate.of(2026, 9, 1))).isFalse();
    }

    @DisplayName("Finds an available team member as replacement for an employee on leave")
    @Test
    void findReplacement_returnsAvailableTeamMember() {
        saveEmployee("eve", "Eve Green", "backend");
        saveEmployee("frank", "Frank Blue", "backend");
        LocalDate leaveDate = LocalDate.of(2026, 10, 5);
        hrService.applyLeave("eve", leaveDate);

        String replacement = hrService.findReplacement("eve", leaveDate);

        assertThat(replacement).isEqualTo("frank");
    }

    @DisplayName("findReplacement throws ResourceNotFoundException when the employee does not exist")
    @Test
    void findReplacement_throwsWhenEmployeeNotFound() {
        assertThatThrownBy(() -> hrService.findReplacement("unknown", LocalDate.now()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @DisplayName("findReplacement throws ResourceNotFoundException when all team members are on leave")
    @Test
    void findReplacement_throwsWhenAllTeamMembersOnLeave() {
        saveEmployee("grace", "Grace Kim", "solo");
        LocalDate leaveDate = LocalDate.of(2026, 11, 1);
        hrService.applyLeave("grace", leaveDate);

        assertThatThrownBy(() -> hrService.findReplacement("grace", leaveDate))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
