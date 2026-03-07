package com.nexacorp.ticket.controller;


import com.nexacorp.ticket.model.Ticket;
import com.nexacorp.ticket.model.TicketPriority;
import com.nexacorp.ticket.model.TicketStatus;
import com.nexacorp.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public Ticket createTicket(@RequestParam String title,
                               @RequestParam String description,
                               @RequestParam TicketPriority priority,
                               @RequestParam String assignee) {

        return ticketService.createTicket(title, description, priority, assignee);
    }

    @GetMapping
    public List<Ticket> getAllTickets() {
        return ticketService.getAllTickets();
    }

    @GetMapping("/{id}")
    public Ticket getTicket(@PathVariable Long id) {
        return ticketService.getTicket(id);
    }

    @PutMapping("/{id}/status")
    public Ticket updateStatus(@PathVariable Long id,
                               @RequestParam TicketStatus status) {
        return ticketService.updateStatus(id, status);
    }

    @PutMapping("/{id}/assign")
    public Ticket assignTicket(@PathVariable Long id,
                               @RequestParam String assignee) {
        return ticketService.assignTicket(id, assignee);
    }
}