package com.org.ticket.controller;


import com.org.ticket.exception.WriteNotPermittedException;
import com.org.ticket.model.Ticket;
import com.org.ticket.model.TicketPriority;
import com.org.ticket.model.TicketStatus;
import com.org.ticket.security.ActingUserContext;
import com.org.ticket.security.RateLimiter;
import com.org.ticket.security.SecurityProperties;
import com.org.ticket.service.TicketService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST + MCP surface for ticket management.
 *
 * <p>Every mutating endpoint:
 * <ul>
 *   <li>Resolves the acting user from {@link ActingUserContext} (set by the auth filter)</li>
 *   <li>Enforces the write-gate when {@code mcp.security.requireUserForWrites} is enabled</li>
 *   <li>Emits an {@code AUDIT} log line with actor, arguments, outcome and latency</li>
 * </ul>
 * Input validation is enforced declaratively via Jakarta Bean Validation.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final SecurityProperties securityProperties;
    private final RateLimiter rateLimiter;

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    @PostMapping
    public Ticket createTicket(@RequestParam @NotBlank String title,
                               @RequestParam @NotBlank String description,
                               @RequestParam @NotNull TicketPriority priority,
                               @RequestParam @NotBlank String assignee) {

        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();
        try {
            Ticket result = ticketService.createTicket(title, description, priority, assignee);
            log.info("AUDIT createTicket | user={} priority={} assignee={} newId={} outcome=SUCCESS latencyMs={}",
                    actingUser, priority, assignee, result.getId(), elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("AUDIT createTicket | user={} priority={} assignee={} outcome=ERROR latencyMs={} error={}",
                    actingUser, priority, assignee, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    @GetMapping
    public List<Ticket> getAllTickets() {
        log.info("TOOL getAllTickets | user={}", resolveUser());
        return ticketService.getAllTickets();
    }

    @GetMapping("/{id}")
    public Ticket getTicket(@PathVariable @Positive Long id) {
        log.info("TOOL getTicket | user={} id={}", resolveUser(), id);
        return ticketService.getTicket(id);
    }

    @PutMapping("/{id}/status")
    public Ticket updateStatus(@PathVariable @Positive Long id,
                               @RequestParam @NotNull TicketStatus status) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();
        try {
            Ticket result = ticketService.updateStatus(id, status);
            log.info("AUDIT updateStatus | user={} id={} status={} outcome=SUCCESS latencyMs={}",
                    actingUser, id, status, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("AUDIT updateStatus | user={} id={} status={} outcome=ERROR latencyMs={} error={}",
                    actingUser, id, status, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    // ─────────────────────────────── helpers ─────────────────────────────────

    @PutMapping("/{id}/assign")
    public Ticket assignTicket(@PathVariable @Positive Long id,
                               @RequestParam @NotBlank String assignee) {
        String actingUser = resolveUser();
        enforceWriteGate(actingUser);
        long start = System.nanoTime();
        try {
            Ticket result = ticketService.assignTicket(id, assignee);
            log.info("AUDIT assignTicket | user={} id={} assignee={} outcome=SUCCESS latencyMs={}",
                    actingUser, id, assignee, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("AUDIT assignTicket | user={} id={} assignee={} outcome=ERROR latencyMs={} error={}",
                    actingUser, id, assignee, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    private String resolveUser() {
        String user = ActingUserContext.get();
        return (user != null && !user.isBlank()) ? user : securityProperties.getDefaultUser();
    }

    private void enforceWriteGate(String actingUser) {
        if (securityProperties.isRequireUserForWrites()
                && securityProperties.getDefaultUser().equals(actingUser)) {
            throw new WriteNotPermittedException(
                    "Write operations require an explicit X-Acting-User header. "
                            + "Default user '" + actingUser + "' is not permitted to perform mutations.");
        }
        if (!rateLimiter.tryAcquireWrite(actingUser)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Write rate limit exceeded (10 writes/min) for user " + actingUser);
        }
    }
}
