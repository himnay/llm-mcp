package com.org.travel.service;

import com.org.travel.amadeus.AmadeusFlightClient;
import com.org.travel.amadeus.model.FlightOffersResponse;
import com.org.travel.amadeus.model.FlightOffersResponse.FlightOffer;
import com.org.travel.amadeus.model.FlightOffersResponse.Itinerary;
import com.org.travel.amadeus.model.FlightOffersResponse.Segment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates the Amadeus flight search and formats the result as a
 * human-readable text block suitable for LLM consumption.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlightSearchService {

    private final AmadeusFlightClient flightClient;

    public String searchAndFormat(String origin, String destination,
                                  String departureDate, int adults, int maxResults) {

        FlightOffersResponse response = flightClient.searchFlights(
                origin.toUpperCase(), destination.toUpperCase(),
                departureDate, adults, maxResults);

        if (response == null || CollectionUtils.isEmpty(response.getData())) {
            return "No flights found for " + origin + " → " + destination + " on " + departureDate + ".";
        }

        Map<String, String> carriers = response.getDictionaries() != null
                ? response.getDictionaries().getCarriers()
                : Map.of();

        StringBuilder sb = new StringBuilder();
        sb.append("Flight availability: ").append(origin.toUpperCase())
                .append(" → ").append(destination.toUpperCase())
                .append(" | Date: ").append(departureDate)
                .append(" | Adults: ").append(adults)
                .append("\n")
                .append("=".repeat(60))
                .append("\n");

        List<FlightOffer> offers = response.getData();
        for (int i = 0; i < offers.size(); i++) {
            FlightOffer offer = offers.get(i);
            sb.append("\nOption ").append(i + 1).append(":\n");

            for (Itinerary itinerary : offer.getItineraries()) {
                List<Segment> segments = itinerary.getSegments();
                int stops = segments.size() - 1;
                sb.append("  Duration: ").append(formatDuration(itinerary.getDuration()))
                        .append(" | Stops: ").append(stops == 0 ? "Non-stop" : stops + " stop(s)").append("\n");

                for (Segment seg : segments) {
                    String airlineName = carriers.getOrDefault(seg.getCarrierCode(), seg.getCarrierCode());
                    sb.append("  ").append(seg.getCarrierCode()).append(seg.getNumber())
                            .append(" (").append(airlineName).append(")")
                            .append(" | ").append(seg.getDeparture().getIataCode())
                            .append(terminalSuffix(seg.getDeparture().getTerminal()))
                            .append(" ").append(formatDateTime(seg.getDeparture().getAt()))
                            .append(" → ").append(seg.getArrival().getIataCode())
                            .append(terminalSuffix(seg.getArrival().getTerminal()))
                            .append(" ").append(formatDateTime(seg.getArrival().getAt()))
                            .append("\n");
                }
            }

            if (offer.getPrice() != null) {
                sb.append("  Price: ").append(offer.getPrice().getGrandTotal())
                        .append(" ").append(offer.getPrice().getCurrency())
                        .append(" (per traveller)\n");
            }
            if (offer.getNumberOfBookableSeats() != null) {
                sb.append("  Seats available: ").append(offer.getNumberOfBookableSeats()).append("\n");
            }
            if (offer.getLastTicketingDate() != null) {
                sb.append("  Book by: ").append(offer.getLastTicketingDate()).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatDuration(String isoDuration) {
        if (isoDuration == null) return "N/A";
        // PT2H30M → 2h 30m
        return isoDuration.replace("PT", "").replace("H", "h ").replace("M", "m").trim();
    }

    private String formatDateTime(String isoDateTime) {
        if (isoDateTime == null) return "N/A";
        // 2024-11-01T06:30:00 → 2024-11-01 06:30
        return isoDateTime.replace("T", " ").substring(0, Math.min(16, isoDateTime.length()));
    }

    private String terminalSuffix(String terminal) {
        return (terminal != null && !terminal.isBlank()) ? " T" + terminal : "";
    }
}
