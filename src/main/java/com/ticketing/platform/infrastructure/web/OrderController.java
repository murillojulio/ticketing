package com.ticketing.platform.infrastructure.web;

import com.ticketing.platform.application.model.CreateOrderCommand;
import com.ticketing.platform.application.port.in.OrderUseCase;
import com.ticketing.platform.infrastructure.web.dto.CreateOrderRequest;
import com.ticketing.platform.infrastructure.web.dto.OrderResponse;
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
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderUseCase orderUseCase;

    public OrderController(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('order:write')")
    public Mono<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderUseCase.createOrder(
                new CreateOrderCommand(
                        request.eventId(),
                        request.customerId(),
                        request.quantity()))
                .map(OrderResponse::from);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('order:read')")
    public Mono<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return orderUseCase.getOrder(orderId).map(OrderResponse::from);
    }
}
