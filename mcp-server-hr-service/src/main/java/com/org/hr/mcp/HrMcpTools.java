package com.org.hr.mcp;

import com.org.hr.config.McpOutputProperties;
import com.org.hr.config.SecurityProperties;
import com.org.hr.config.ToolOutputUtil;
import com.org.hr.security.ActingUserContext;
import com.org.hr.service.HRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * MCP tool implementations for HR operations.
 *
 * <p>Each method:
 * <ul>
 *   <li>Validates inputs (null/blank guards, date parsing).</li>
 *   <li>Logs tool name, acting user, sanitised args, outcome and latency.</li>
 *   <li>Clears no ThreadLocal state itself – that is done by the filter.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class HrMcpTools {

    private final HRService hrService;
    private final SecurityProperties securityProperties;
    private final McpOutputProperties mcpOutputProperties;

    // -------------------------------------------------------------------------
    // applyLeave — WRITE tool
    // -------------------------------------------------------------------------

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid date format '" + date + "' — expected yyyy-MM-dd", ex);
        }
    }

    // -------------------------------------------------------------------------
    // findReplacement — READ tool
    // -------------------------------------------------------------------------

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @McpTool(
            name = "applyLeave",
            description = "Apply leave for a user on a specific ISO-8601 date (yyyy-MM-dd)"
    )
    public String applyLeave(
            @McpToolParam(description = "The user name") String username,
            @McpToolParam(description = "The date to apply leave on (yyyy-MM-dd)") String date) {

        // --- Input validation ---
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("date must not be blank");
        }
        LocalDate leaveDate = parseDate(date);

        String actingUser = ActingUserContext.get();
        if (actingUser == null) {
            actingUser = securityProperties.getDefaultUser();
        }

        // --- Write gate ---
        if (securityProperties.isRequireUserForWrites()
                && securityProperties.getDefaultUser().equalsIgnoreCase(actingUser)) {
            throw new IllegalStateException(
                    "applyLeave requires an identified acting user; "
                            + "provide the X-Acting-User header");
        }

        long start = System.nanoTime();
        String outcome = "success";
        try {
            hrService.applyLeave(username, leaveDate);
            String result = "Leave applied for " + username + " on " + date;
            log.info("[AUDIT] tool=applyLeave actingUser={} username={} date={} outcome=success latencyMs={}",
                    actingUser, username, date, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            outcome = "failure:" + ex.getClass().getSimpleName();
            log.warn("[AUDIT] tool=applyLeave actingUser={} username={} date={} outcome={} latencyMs={}",
                    actingUser, username, date, outcome, elapsedMs(start));
            throw ex;
        }
    }

    @McpTool(
            name = "findReplacement",
            description = "Find a replacement employee for a user on a specific date - ISO-8601 date format (yyyy-MM-dd)"
    )
    public String findReplacement(
            @McpToolParam(description = "The user name") String username,
            @McpToolParam(description = "The date to find a replacement for (yyyy-MM-dd)") String date) {

        // --- Input validation ---
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("date must not be blank");
        }
        LocalDate leaveDate = parseDate(date);

        String actingUser = ActingUserContext.get();
        if (actingUser == null) {
            actingUser = securityProperties.getDefaultUser();
        }

        long start = System.nanoTime();
        try {
            String result = hrService.findReplacement(username, leaveDate);
            result = ToolOutputUtil.cap(result, mcpOutputProperties.getMaxChars());
            log.info("[AUDIT] tool=findReplacement actingUser={} username={} date={} outcome=success latencyMs={}",
                    actingUser, username, date, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.warn("[AUDIT] tool=findReplacement actingUser={} username={} date={} outcome=failure:{} latencyMs={}",
                    actingUser, username, date, ex.getClass().getSimpleName(), elapsedMs(start));
            throw ex;
        }
    }
}
