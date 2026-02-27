package com.ticketing.platform.infrastructure.web;

import com.ticketing.platform.application.model.CreateEventCommand;
import com.ticketing.platform.application.port.in.EventUseCase;
import com.ticketing.platform.infrastructure.web.dto.CreateEventRequest;
import com.ticketing.platform.infrastructure.web.dto.EventAvailabilityResponse;
import com.ticketing.platform.infrastructure.web.dto.EventResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventUseCase eventUseCase;

    public EventController(EventUseCase eventUseCase) {
        this.eventUseCase = eventUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('event:write')")
    public Mono<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventUseCase.createEvent(
                new CreateEventCommand(
                        request.name(),
                        request.date(),
                        request.venue(),
                        request.totalCapacity()))
                .map(EventResponse::from);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('event:read')")
    public Flux<EventResponse> listEvents() {
        return eventUseCase.listEvents().map(EventResponse::from);
    }

    @GetMapping("/{eventId}/availability")
    @PreAuthorize("hasAuthority('event:read')")
    public Mono<EventAvailabilityResponse> getAvailability(@PathVariable UUID eventId) {
        return eventUseCase.getAvailability(eventId).map(EventAvailabilityResponse::from);
    }
}
