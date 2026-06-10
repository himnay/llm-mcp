package com.org.ticket.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the enum-based State pattern in {@link TicketStatus}. */
class TicketStatusTransitionTest {

    @Test
    void openCanMoveToInProgressAndClosed() {
        assertThat(TicketStatus.OPEN.canTransitionTo(TicketStatus.IN_PROGRESS)).isTrue();
        assertThat(TicketStatus.OPEN.canTransitionTo(TicketStatus.CLOSED)).isTrue();
    }

    @Test
    void inProgressCanMoveToClosedOrBackToOpen() {
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.CLOSED)).isTrue();
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.OPEN)).isTrue();
    }

    @Test
    void closedCanOnlyBeReopened() {
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.OPEN)).isTrue();
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.IN_PROGRESS)).isFalse();
    }

    @Test
    void sameStatusIsAlwaysAllowed() {
        for (TicketStatus status : TicketStatus.values()) {
            assertThat(status.canTransitionTo(status)).isTrue();
        }
    }
}
