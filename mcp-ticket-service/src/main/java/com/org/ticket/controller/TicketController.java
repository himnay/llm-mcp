package com.org.ticket.controller;


import com.org.ticket.model.Ticket;
import com.org.ticket.model.TicketPriority;
import com.org.ticket.model.TicketStatus;
import com.org.ticket.service.TicketService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Validated
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public Ticket createTicket(@RequestParam @NotBlank String title,
                               @RequestParam @NotBlank String description,
                               @RequestParam @NotNull TicketPriority priority,
                               @RequestParam @NotBlank String assignee) {

        return ticketService.createTicket(title, description, priority, assignee);
    }

    @GetMapping
    public List<Ticket> getAllTickets() {
        return ticketService.getAllTickets();
    }

    @GetMapping("/{id}")
    public Ticket getTicket(@PathVariable @Positive Long id) {
        return ticketService.getTicket(id);
    }

    @PutMapping("/{id}/status")
    public Ticket updateStatus(@PathVariable @Positive Long id,
                               @RequestParam @NotNull TicketStatus status) {
        return ticketService.updateStatus(id, status);
    }

    @PutMapping("/{id}/assign")
    public Ticket assignTicket(@PathVariable @Positive Long id,
                               @RequestParam @NotBlank String assignee) {
        return ticketService.assignTicket(id, assignee);
    }
}