package com.ticketing.platform.application.port.in;

import reactor.core.publisher.Mono;

public interface ReservationReleaseUseCase {

    Mono<Void> releaseExpiredReservations();
}
