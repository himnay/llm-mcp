package com.org.travel.service;

import com.org.travel.amadeus.AmadeusFlightClient;
import com.org.travel.amadeus.AmadeusTokenService;
import com.org.travel.amadeus.model.FlightOffersResponse;
import com.org.travel.amadeus.model.FlightOffersResponse.FlightOffer;
import com.org.travel.amadeus.model.FlightOffersResponse.Itinerary;
import com.org.travel.amadeus.model.FlightOffersResponse.Price;
import com.org.travel.amadeus.model.FlightOffersResponse.Segment;
import com.org.travel.amadeus.model.FlightOffersResponse.AirportTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightSearchServiceTest {

    @Mock
    private AmadeusFlightClient flightClient;

    @Mock
    private AmadeusTokenService tokenService;

    private FlightSearchService flightSearchService;

    @BeforeEach
    void setUp() {
        flightSearchService = new FlightSearchService(flightClient);
    }

    @DisplayName("Formats flight search results with route, date, and price details")
    @Test
    void searchAndFormat_returnsFormattedFlights_forValidRoute() {
        FlightOffersResponse response = buildSampleResponse();
        when(flightClient.searchFlights(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(response);

        String result = flightSearchService.searchAndFormat("DUB", "LHR", "2026-07-01", 1, 5);

        assertThat(result).contains("DUB");
        assertThat(result).contains("LHR");
        assertThat(result).contains("2026-07-01");
        assertThat(result).contains("Option 1");
        assertThat(result).contains("100.00");
    }

    @DisplayName("Returns a no-flights message when the response data is empty")
    @Test
    void searchAndFormat_returnsNoFlightsMessage_whenResponseEmpty() {
        FlightOffersResponse empty = new FlightOffersResponse();
        empty.setData(List.of());
        when(flightClient.searchFlights(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(empty);

        String result = flightSearchService.searchAndFormat("DUB", "XYZ", "2026-07-01", 1, 5);

        assertThat(result).contains("No flights found");
    }

    @DisplayName("Uppercases origin and destination codes before calling the flight client")
    @Test
    void searchAndFormat_callsFlightClientWithUppercasedCodes() {
        when(flightClient.searchFlights("DUB", "LHR", "2026-07-01", 2, 3))
                .thenReturn(buildSampleResponse());

        flightSearchService.searchAndFormat("dub", "lhr", "2026-07-01", 2, 3);

        verify(flightClient).searchFlights("DUB", "LHR", "2026-07-01", 2, 3);
    }

    private FlightOffersResponse buildSampleResponse() {
        AirportTime dep = new AirportTime();
        dep.setIataCode("DUB");
        dep.setAt("2026-07-01T06:00:00");

        AirportTime arr = new AirportTime();
        arr.setIataCode("LHR");
        arr.setAt("2026-07-01T07:30:00");

        Segment segment = new Segment();
        segment.setCarrierCode("EI");
        segment.setNumber("123");
        segment.setDeparture(dep);
        segment.setArrival(arr);

        Itinerary itinerary = new Itinerary();
        itinerary.setDuration("PT1H30M");
        itinerary.setSegments(List.of(segment));

        Price price = new Price();
        price.setGrandTotal("100.00");
        price.setCurrency("EUR");

        FlightOffer offer = new FlightOffer();
        offer.setItineraries(List.of(itinerary));
        offer.setPrice(price);
        offer.setNumberOfBookableSeats(5);

        FlightOffersResponse response = new FlightOffersResponse();
        response.setData(List.of(offer));

        FlightOffersResponse.Dictionaries dictionaries = new FlightOffersResponse.Dictionaries();
        dictionaries.setCarriers(Map.of("EI", "Aer Lingus"));
        response.setDictionaries(dictionaries);

        return response;
    }
}
