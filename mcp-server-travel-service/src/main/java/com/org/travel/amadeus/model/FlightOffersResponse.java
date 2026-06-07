package com.org.travel.amadeus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightOffersResponse {

    private List<FlightOffer> data;
    private Dictionaries dictionaries;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FlightOffer {
        private String id;
        private Integer numberOfBookableSeats;
        private List<Itinerary> itineraries;
        private Price price;
        private String lastTicketingDate;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Itinerary {
        private String duration;
        private List<Segment> segments;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Segment {
        private AirportTime departure;
        private AirportTime arrival;
        private String carrierCode;
        private String number;
        private String duration;
        private Integer numberOfStops;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AirportTime {
        private String iataCode;
        private String terminal;
        private String at;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        private String currency;
        private String grandTotal;
        private String total;
        private String base;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dictionaries {
        private Map<String, String> carriers;
        private Map<String, String> aircraft;
    }
}
