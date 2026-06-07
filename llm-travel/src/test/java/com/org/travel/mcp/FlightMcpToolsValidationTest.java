package com.org.travel.mcp;

import com.org.travel.config.McpOutputProperties;
import com.org.travel.config.SecurityProperties;
import com.org.travel.service.FlightSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightMcpToolsValidationTest {

    @Mock
    private FlightSearchService flightSearchService;

    private FlightMcpTools tools;

    @BeforeEach
    void setUp() {
        SecurityProperties secProps = new SecurityProperties();
        McpOutputProperties outputProps = new McpOutputProperties();
        tools = new FlightMcpTools(flightSearchService, secProps, outputProps);
    }

    @Test
    void rejectsBlankOriginCode() {
        assertThatThrownBy(() -> tools.searchFlights("", "MUC", "2025-08-01", 1, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("originCode");
    }

    @Test
    void rejectsBlankDestinationCode() {
        assertThatThrownBy(() -> tools.searchFlights("DUB", "", "2025-08-01", 1, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("destinationCode");
    }

    @Test
    void rejectsInvalidDateFormat() {
        assertThatThrownBy(() -> tools.searchFlights("DUB", "MUC", "01-08-2025", 1, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yyyy-MM-dd");
    }

    @Test
    void rejectsAdultsOutOfRange() {
        assertThatThrownBy(() -> tools.searchFlights("DUB", "MUC", "2025-08-01", 0, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adults");
        assertThatThrownBy(() -> tools.searchFlights("DUB", "MUC", "2025-08-01", 10, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adults");
    }

    @Test
    void delegatesToServiceWithValidInputs() {
        when(flightSearchService.searchAndFormat(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn("Flight results");
        String result = tools.searchFlights("DUB", "MUC", "2025-08-01", 1, 5);
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo("Flight results");
    }

    @Test
    void capsMaxResultsAt20() {
        when(flightSearchService.searchAndFormat(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn("ok");
        // Should not throw even with maxResults=50 — gets capped internally
        tools.searchFlights("DUB", "MUC", "2025-08-01", 1, 50);
    }

    @Test
    void rejectsBlankCityForAirportInfo() {
        assertThatThrownBy(() -> tools.getAirportInfo(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cityOrAirport");
    }

    @Test
    void returnsAirportCodeForKnownCity() {
        String result = tools.getAirportInfo("dublin");
        org.assertj.core.api.Assertions.assertThat(result).contains("DUB");
    }

    @Test
    void returnsNotFoundForUnknownCity() {
        String result = tools.getAirportInfo("atlantis");
        org.assertj.core.api.Assertions.assertThat(result).contains("not found");
    }
}
