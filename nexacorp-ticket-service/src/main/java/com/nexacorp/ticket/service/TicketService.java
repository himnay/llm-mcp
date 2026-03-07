package com.nexacorp.ticket.service;


import com.nexacorp.ticket.model.Ticket;
import com.nexacorp.ticket.model.TicketPriority;
import com.nexacorp.ticket.model.TicketStatus;
import com.nexacorp.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    public Ticket createTicket(String title,
                               String description,
                               TicketPriority priority,
                               String assignee) {

        Ticket ticket = Ticket.builder()
                .title(title)
                .description(description)
                .status(TicketStatus.OPEN)
                .priority(priority)
                .assignee(assignee)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return ticketRepository.save(ticket);
    }

    public Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Ticket updateStatus(Long id, TicketStatus status) {
        Ticket ticket = getTicket(id);
        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public Ticket assignTicket(Long id, String assignee) {
        Ticket ticket = getTicket(id);
        ticket.setAssignee(assignee);
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }
}