package com.org.travel.mcp;

import com.org.travel.exception.InvalidToolArgumentException;
import com.org.travel.config.McpOutputProperties;
import com.org.travel.config.SecurityProperties;
import com.org.travel.service.FlightSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Throws when the origin code is blank")
    void rejectsBlankOriginCode() {
        assertThatThrownBy(() -> tools.searchFlights("", "MUC", "2025-08-01", 1, 5))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("originCode");
    }

    @Test
    @DisplayName("Throws when the destination code is blank")
    void rejectsBlankDestinationCode() {
        assertThatThrownBy(() -> tools.searchFlights("DUB", "", "2025-08-01", 1, 5))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("destinationCode");
    }

    @Test
    @DisplayName("Throws when the departure date is not in yyyy-MM-dd format")
    void rejectsInvalidDateFormat() {
        assertThatThrownBy(() -> tools.searchFlights("DUB", "MUC", "01-08-2025", 1, 5))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("yyyy-MM-dd");
    }

    @Test
    @DisplayName("Throws when the adults count is below or above the allowed range")
    void rejectsAdultsOutOfRange() {
        assertThatThrownBy(() -> tools.searchFlights("DUB", "MUC", "2025-08-01", 0, 5))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("adults");
        assertThatThrownBy(() -> tools.searchFlights("DUB", "MUC", "2025-08-01", 10, 5))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("adults");
    }

    @Test
    @DisplayName("Delegates to the flight search service when inputs are valid")
    void delegatesToServiceWithValidInputs() {
        when(flightSearchService.searchAndFormat(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn("Flight results");
        String result = tools.searchFlights("DUB", "MUC", "2025-08-01", 1, 5);
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo("Flight results");
    }

    @Test
    @DisplayName("Does not throw when maxResults exceeds 20, capping it internally")
    void capsMaxResultsAt20() {
        when(flightSearchService.searchAndFormat(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn("ok");
        // Should not throw even with maxResults=50 — gets capped internally
        tools.searchFlights("DUB", "MUC", "2025-08-01", 1, 50);
    }

    @Test
    @DisplayName("Throws when the city or airport argument is blank")
    void rejectsBlankCityForAirportInfo() {
        assertThatThrownBy(() -> tools.getAirportInfo(""))
                .isInstanceOf(InvalidToolArgumentException.class)
                .hasMessageContaining("cityOrAirport");
    }

    @Test
    @DisplayName("Returns the airport code for a recognized city name")
    void returnsAirportCodeForKnownCity() {
        String result = tools.getAirportInfo("dublin");
        org.assertj.core.api.Assertions.assertThat(result).contains("DUB");
    }

    @Test
    @DisplayName("Returns a not-found message for an unrecognized city or airport")
    void returnsNotFoundForUnknownCity() {
        String result = tools.getAirportInfo("atlantis");
        org.assertj.core.api.Assertions.assertThat(result).contains("not found");
    }
}
