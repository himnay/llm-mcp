package com.org.ticket.mcp;

import com.org.ticket.model.Ticket;
import com.org.ticket.service.TicketService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class TicketPromptProvider {
    private final TicketService ticketService;

    @McpPrompt(
            name = "analyze-tickets",
            description = "Instructions for analyzing Org support tickets."
    )
    public McpSchema.GetPromptResult analyzeTicketsPrompt() {

        List<Ticket> tickets = ticketService.getAllTickets();
        StringBuilder ticketSummary = new StringBuilder();
        for (Ticket t : tickets) {
            ticketSummary.append("Ticket ")
                    .append(t.getId()).append(": ").append(t.getTitle())
                    .append(" | Priority=").append(t.getPriority())
                    .append(" | Status=").append(t.getStatus())
                    .append("\n");
        }
        String prompt = """
                You are a Org operations analyst.

                Analyze the current support tickets and provide insights.

                Tickets:
                """ + ticketSummary + """

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
