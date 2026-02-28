package com.ticketing.platform.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.exception.InsufficientInventoryException;

public record Event(
    UUID id,
    String name,
    Instant date,
    String venue,
    int totalCapacity,
    int availableTickets,
    int reservedTickets,
    int soldTickets,
    int complimentaryTickets,
    long version
) {

    public Event {
        if (id == null) {
            throw new DomainException("Event id is required");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("Event name is required");
        }
        if (date == null) {
            throw new DomainException("Event date is required");
        }
        if (venue == null || venue.isBlank()) {
            throw new DomainException("Event venue is required");
        }
        if (totalCapacity <= 0) {
            throw new DomainException("Event capacity must be greater than zero");
        }
        if (availableTickets < 0 || reservedTickets < 0 || soldTickets < 0 || complimentaryTickets < 0) {
            throw new DomainException("Ticket counters cannot be negative");
        }
        if (availableTickets + reservedTickets + soldTickets + complimentaryTickets != totalCapacity) {
            throw new DomainException("Ticket counters must add up to total capacity");
        }
    }

    public static Event create(UUID id, String name, Instant date, String venue, int totalCapacity) {
        return new Event(
            id,
            name,
            date,
            venue,
            totalCapacity,
            totalCapacity,
            0,
            0,
            0,
            0
        );
    }

    public Event reserve(int quantity) {
        validateQuantity(quantity);
        if (availableTickets < quantity) {
            throw new InsufficientInventoryException("Not enough available tickets for reservation");
        }
        return new Event(
            id,
            name,
            date,
            venue,
            totalCapacity,
            availableTickets - quantity,
            reservedTickets + quantity,
            soldTickets,
            complimentaryTickets,
            version + 1
        );
    }

    public Event releaseReservation(int quantity) {
        validateQuantity(quantity);
        if (reservedTickets < quantity) {
            throw new DomainException("Cannot release more tickets than currently reserved");
        }
        return new Event(
            id,
            name,
            date,
            venue,
            totalCapacity,
            availableTickets + quantity,
            reservedTickets - quantity,
            soldTickets,
            complimentaryTickets,
            version + 1
        );
    }

    public Event confirmSale(int quantity) {
        validateQuantity(quantity);
        if (reservedTickets < quantity) {
            throw new DomainException("Cannot confirm sale without reserved tickets");
        }
        return new Event(
            id,
            name,
            date,
            venue,
            totalCapacity,
            availableTickets,
            reservedTickets - quantity,
            soldTickets + quantity,
            complimentaryTickets,
            version + 1
        );
    }

    public Event grantComplimentary(int quantity) {
        validateQuantity(quantity);
        if (availableTickets < quantity) {
            throw new InsufficientInventoryException("Not enough tickets for complimentary allocation");
        }
        return new Event(
            id,
            name,
            date,
            venue,
            totalCapacity,
            availableTickets - quantity,
            reservedTickets,
            soldTickets,
            complimentaryTickets + quantity,
            version + 1
        );
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new DomainException("Quantity must be greater than zero");
        }
    }
}
