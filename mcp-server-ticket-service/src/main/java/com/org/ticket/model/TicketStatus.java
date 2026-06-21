package com.org.ticket.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * State (GoF, enum-based): each status owns the set of statuses it may legally
 * move to, so the ticket lifecycle (OPEN → IN_PROGRESS → CLOSED, with reopen)
 * is enforced in one place instead of scattered if-checks.
 */
public enum TicketStatus {
    OPEN {
        @Override
        public Set<TicketStatus> allowedTransitions() {
            return EnumSet.of(IN_PROGRESS, CLOSED);
        }
    },
    IN_PROGRESS {
        @Override
        public Set<TicketStatus> allowedTransitions() {
            return EnumSet.of(OPEN, CLOSED);
        }
    },
    CLOSED {
        @Override
        public Set<TicketStatus> allowedTransitions() {
            return EnumSet.of(OPEN); // reopen
        }
    };

    public abstract Set<TicketStatus> allowedTransitions();

    /**
     * A no-op transition (same status) is always permitted.
     */
    public boolean canTransitionTo(TicketStatus target) {
        return this == target || allowedTransitions().contains(target);
    }
}
