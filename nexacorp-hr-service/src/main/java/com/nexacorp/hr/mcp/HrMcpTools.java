package com.nexacorp.hr.mcp;

import com.nexacorp.hr.service.HRService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class HrMcpTools {

    private final HRService hrService;

    @McpTool(
            name = "applyLeave",
            description = "Apply leave for a user on a specific ISO-8601 date (yyyy-MM-dd)"
    )
    public String applyLeave(@McpToolParam(description = "The user name") String username,
                             @McpToolParam(description = "The date to apply leave on")  String date) {

        hrService.applyLeave(username, java.time.LocalDate.parse(date));
        return "Leave applied for " + username + " on " + date;
    }
}
