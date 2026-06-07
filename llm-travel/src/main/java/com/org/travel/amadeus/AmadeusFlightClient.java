package com.org.travel.amadeus;

import com.org.travel.amadeus.model.FlightOffersResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin HTTP client for the Amadeus v2 flight-offers endpoint.
 * Token injection and retry concerns are handled by the calling service.
 */
@Slf4j
@Component
public class AmadeusFlightClient {

    private final RestClient restClient;
    private final AmadeusTokenService tokenService;

    public AmadeusFlightClient(@Qualifier("amadeusRestClient") RestClient restClient,
                               AmadeusTokenService tokenService) {
        this.restClient = restClient;
        this.tokenService = tokenService;
    }

    /**
     * Searches for one-way flight offers.
     *
     * @param origin        IATA airport code (e.g. DUB)
     * @param destination   IATA airport code (e.g. MUC)
     * @param departureDate ISO-8601 date (yyyy-MM-dd)
     * @param adults        number of adult passengers (1–9)
     * @param maxResults    maximum number of offers to return (1–20)
     */
    public FlightOffersResponse searchFlights(String origin, String destination,
                                              String departureDate, int adults, int maxResults) {
        String token = tokenService.getToken();
        log.debug("Calling Amadeus flight-offers | {}→{} on {} adults={} max={}",
                origin, destination, departureDate, adults, maxResults);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/shopping/flight-offers")
                        .queryParam("originLocationCode", origin)
                        .queryParam("destinationLocationCode", destination)
                        .queryParam("departureDate", departureDate)
                        .queryParam("adults", adults)
                        .queryParam("max", maxResults)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(FlightOffersResponse.class);
    }
}
