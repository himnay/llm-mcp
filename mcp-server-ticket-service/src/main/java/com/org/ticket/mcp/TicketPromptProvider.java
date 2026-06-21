package com.org.ticket.mcp;

import com.org.ticket.model.Ticket;
import com.org.ticket.security.ActingUserContext;
import com.org.ticket.service.TicketService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class TicketPromptProvider {
    private final TicketService ticketService;

    @Value("${mcp.output.max-chars:8000}")
    private int maxOutputChars;

    @McpPrompt(
            name = "analyze-tickets",
            description = "Instructions for analyzing Org support tickets."
    )
    public McpSchema.GetPromptResult analyzeTicketsPrompt() {

        String actingUser = ActingUserContext.get();
        List<Ticket> tickets = ticketService.getAllTickets();
        StringBuilder ticketSummary = new StringBuilder();
        for (Ticket t : tickets) {
            ticketSummary.append("Ticket ")
                    .append(t.getId()).append(": ").append(t.getTitle())
                    .append(" | Priority=").append(t.getPriority())
                    .append(" | Status=").append(t.getStatus())
                    .append("\n");
        }
        // Cap the ticket summary so a large backlog cannot flood the LLM context window.
        String cappedSummary = OutputSizeCapUtil.cap(ticketSummary.toString(), maxOutputChars);
        log.info("PROMPT analyze-tickets | user={} ticketCount={}", actingUser, tickets.size());
        String prompt = """
                You are a Org operations analyst.
                
                Analyze the current support tickets and provide insights.
                
                Tickets:
                """ + cappedSummary + """
                
                Provide:
                1. Overall ticket situation
                2. High priority issues
                3. Possible operational risks
                4. Recommended next actions
                """;

        return new McpSchema.GetPromptResult("Analyze Tickets",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(prompt))));
    }
}
