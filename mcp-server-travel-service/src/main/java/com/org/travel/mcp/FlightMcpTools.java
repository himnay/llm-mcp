package com.org.travel.mcp;

import com.org.travel.config.McpOutputProperties;
import com.org.travel.config.SecurityProperties;
import com.org.travel.config.ToolOutputUtil;
import com.org.travel.security.ActingUserContext;
import com.org.travel.service.FlightSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * MCP tools for flight availability search via the Amadeus API.
 *
 * <p>Each tool:
 * <ul>
 *   <li>Validates inputs before calling downstream.</li>
 *   <li>Logs tool name, acting user, sanitised args, outcome, and latency.</li>
 *   <li>Caps output via {@link ToolOutputUtil} to prevent context bloat.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FlightMcpTools {

    private static final java.util.Map<String, String> AIRPORT_LOOKUP = new java.util.LinkedHashMap<>() {{
        put("dublin", "DUB — Dublin Airport, Ireland");
        put("munich", "MUC — Munich Airport, Germany");
        put("london heathrow", "LHR — London Heathrow, United Kingdom");
        put("london gatwick", "LGW — London Gatwick, United Kingdom");
        put("amsterdam", "AMS — Amsterdam Schiphol, Netherlands");
        put("paris cdg", "CDG — Paris Charles de Gaulle, France");
        put("paris orly", "ORY — Paris Orly, France");
        put("frankfurt", "FRA — Frankfurt Airport, Germany");
        put("berlin", "BER — Berlin Brandenburg, Germany");
        put("madrid", "MAD — Madrid Barajas, Spain");
        put("barcelona", "BCN — Barcelona El Prat, Spain");
        put("rome", "FCO — Rome Fiumicino, Italy");
        put("milan", "MXP — Milan Malpensa, Italy");
        put("new york jfk", "JFK — John F. Kennedy International, USA");
        put("new york newark", "EWR — Newark Liberty International, USA");
        put("los angeles", "LAX — Los Angeles International, USA");
        put("chicago", "ORD — Chicago O'Hare International, USA");
        put("miami", "MIA — Miami International, USA");
        put("dubai", "DXB — Dubai International, UAE");
        put("singapore", "SIN — Singapore Changi, Singapore");
        put("sydney", "SYD — Sydney Kingsford Smith, Australia");
        put("tokyo", "NRT — Tokyo Narita, Japan");
        put("hong kong", "HKG — Hong Kong International, China");
        put("toronto", "YYZ — Toronto Pearson, Canada");
    }};
    private final FlightSearchService flightSearchService;
    private final SecurityProperties securityProperties;
    private final McpOutputProperties mcpOutputProperties;

    private static void validateDate(String date) {
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid date format '" + date + "' — expected yyyy-MM-dd", ex);
        }
    }

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    @Tool(
            name = "searchFlights",
            description = """
                    Search for available flights between two airports using the Amadeus API.
                    Returns flight options with airline, departure/arrival times, duration,
                    number of stops, price, and seat availability.
                    Airport codes must be IATA format (e.g. DUB for Dublin, MUC for Munich,
                    LHR for London Heathrow, JFK for New York).
                    """
    )
    public String searchFlights(
            @ToolParam(description = "Origin airport IATA code (e.g. DUB for Dublin)")
            String originCode,
            @ToolParam(description = "Destination airport IATA code (e.g. MUC for Munich)")
            String destinationCode,
            @ToolParam(description = "Departure date in ISO-8601 format (yyyy-MM-dd, e.g. 2025-08-15)")
            String departureDate,
            @ToolParam(description = "Number of adult passengers (1 to 9)")
            int adults,
            @ToolParam(description = "Maximum number of flight options to return (1 to 20, default 5)")
            int maxResults) {

        if (originCode == null || originCode.isBlank()) {
            throw new IllegalArgumentException("originCode must not be blank");
        }
        if (destinationCode == null || destinationCode.isBlank()) {
            throw new IllegalArgumentException("destinationCode must not be blank");
        }
        if (departureDate == null || departureDate.isBlank()) {
            throw new IllegalArgumentException("departureDate must not be blank");
        }
        validateDate(departureDate);
        if (adults < 1 || adults > 9) {
            throw new IllegalArgumentException("adults must be between 1 and 9");
        }
        int cappedMax = Math.max(1, Math.min(20, maxResults));

        String actingUser = ActingUserContext.get();
        if (actingUser == null) {
            actingUser = securityProperties.getDefaultUser();
        }

        long start = System.nanoTime();
        try {
            String result = flightSearchService.searchAndFormat(
                    originCode, destinationCode, departureDate, adults, cappedMax);
            result = ToolOutputUtil.cap(result, mcpOutputProperties.getMaxChars());
            log.info("[AUDIT] tool=searchFlights actingUser={} origin={} destination={} date={} adults={} outcome=success latencyMs={}",
                    actingUser, originCode, destinationCode, departureDate, adults, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.warn("[AUDIT] tool=searchFlights actingUser={} origin={} destination={} date={} outcome=failure:{} latencyMs={}",
                    actingUser, originCode, destinationCode, departureDate, ex.getClass().getSimpleName(), elapsedMs(start));
            throw ex;
        }
    }

    @Tool(
            name = "getAirportInfo",
            description = """
                    Returns IATA airport codes for common cities to help identify the correct
                    code before calling searchFlights.
                    Examples: Dublin=DUB, Munich=MUC, London Heathrow=LHR, London Gatwick=LGW,
                    Amsterdam=AMS, Paris CDG=CDG, Frankfurt=FRA, New York JFK=JFK,
                    New York Newark=EWR, Los Angeles=LAX, Chicago=ORD, Dubai=DXB,
                    Singapore=SIN, Sydney=SYD, Tokyo=NRT, Hong Kong=HKG.
                    """
    )
    public String getAirportInfo(
            @ToolParam(description = "City or airport name to look up (e.g. Dublin, Munich)")
            String cityOrAirport) {

        if (cityOrAirport == null || cityOrAirport.isBlank()) {
            throw new IllegalArgumentException("cityOrAirport must not be blank");
        }

        // Static lookup for common airports — extend as needed
        String query = cityOrAirport.toLowerCase().trim();
        String result = AIRPORT_LOOKUP.entrySet().stream()
                .filter(e -> e.getKey().contains(query) || query.contains(e.getKey()))
                .map(e -> e.getKey() + " → " + e.getValue())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("Airport code not found for '" + cityOrAirport
                        + "'. Use a standard IATA code directly (3 uppercase letters).");

        log.info("[AUDIT] tool=getAirportInfo actingUser={} query={}", ActingUserContext.get(), cityOrAirport);
        return result;
    }
}
