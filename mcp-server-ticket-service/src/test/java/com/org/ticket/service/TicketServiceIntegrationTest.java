package com.org.ticket.service;

import com.org.ticket.TestcontainersConfiguration;
import com.org.ticket.exception.InvalidStatusTransitionException;
import com.org.ticket.exception.ResourceNotFoundException;
import com.org.ticket.model.Ticket;
import com.org.ticket.model.TicketPriority;
import com.org.ticket.model.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class TicketServiceIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Test
    @DisplayName("Creates a ticket and persists it with a generated ID")
    void createTicket_persistsAndReturnsWithId() {
        Ticket ticket = ticketService.createTicket(
                "Fix login bug", "Users cannot log in after session expires",
                TicketPriority.HIGH, "alice");

        assertThat(ticket.getId()).isNotNull();
        assertThat(ticket.getTitle()).isEqualTo("Fix login bug");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(ticket.getAssignee()).isEqualTo("alice");
    }

    @Test
    @DisplayName("Retrieves a previously persisted ticket by ID")
    void getTicket_returnsPersistedTicket() {
        Ticket created = ticketService.createTicket(
                "Add dark mode", null, TicketPriority.LOW, null);

        Ticket fetched = ticketService.getTicket(created.getId());

        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getTitle()).isEqualTo("Add dark mode");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when the ticket does not exist")
    void getTicket_throwsWhenNotFound() {
        assertThatThrownBy(() -> ticketService.getTicket(999_999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }

    @Test
    @DisplayName("Returns all persisted tickets including newly created ones")
    void getAllTickets_returnsAll() {
        ticketService.createTicket("T1", null, TicketPriority.LOW, null);
        ticketService.createTicket("T2", null, TicketPriority.HIGH, "bob");

        List<Ticket> tickets = ticketService.getAllTickets();
        assertThat(tickets).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Updates ticket status when the transition is valid")
    void updateStatus_validTransition_updatesStatus() {
        Ticket ticket = ticketService.createTicket(
                "Perf issue", null, TicketPriority.MEDIUM, "charlie");

        Ticket updated = ticketService.updateStatus(ticket.getId(), TicketStatus.IN_PROGRESS);

        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Throws InvalidStatusTransitionException when the status transition is invalid")
    void updateStatus_invalidTransition_throwsIllegalArgument() {
        Ticket ticket = ticketService.createTicket(
                "Invalid transition test", null, TicketPriority.LOW, null);
        ticketService.updateStatus(ticket.getId(), TicketStatus.CLOSED);

        assertThatThrownBy(() ->
                ticketService.updateStatus(ticket.getId(), TicketStatus.IN_PROGRESS))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    @DisplayName("Updates the assignee when a ticket is assigned")
    void assignTicket_updatesAssignee() {
        Ticket ticket = ticketService.createTicket(
                "Unassigned task", null, TicketPriority.LOW, null);

        Ticket assigned = ticketService.assignTicket(ticket.getId(), "dave");

        assertThat(assigned.getAssignee()).isEqualTo("dave");
    }
}
