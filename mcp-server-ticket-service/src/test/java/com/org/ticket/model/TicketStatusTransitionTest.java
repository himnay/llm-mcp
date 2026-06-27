package com.org.ticket.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the enum-based State pattern in {@link TicketStatus}.
 */
class TicketStatusTransitionTest {

    @Test
    @DisplayName("Allows OPEN tickets to move to IN_PROGRESS or CLOSED")
    void openCanMoveToInProgressAndClosed() {
        assertThat(TicketStatus.OPEN.canTransitionTo(TicketStatus.IN_PROGRESS)).isTrue();
        assertThat(TicketStatus.OPEN.canTransitionTo(TicketStatus.CLOSED)).isTrue();
    }

    @Test
    @DisplayName("Allows IN_PROGRESS tickets to move to CLOSED or back to OPEN")
    void inProgressCanMoveToClosedOrBackToOpen() {
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.CLOSED)).isTrue();
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.OPEN)).isTrue();
    }

    @Test
    @DisplayName("Allows CLOSED tickets to only transition back to OPEN")
    void closedCanOnlyBeReopened() {
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.OPEN)).isTrue();
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.IN_PROGRESS)).isFalse();
    }

    @Test
    @DisplayName("Allows every status to transition to itself")
    void sameStatusIsAlwaysAllowed() {
        for (TicketStatus status : TicketStatus.values()) {
            assertThat(status.canTransitionTo(status)).isTrue();
        }
    }
}
