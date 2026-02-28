package com.ticketing.platform.infrastructure.scheduler;

import com.ticketing.platform.application.port.in.ReservationReleaseUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationExpirationScheduler.class);

    private final ReservationReleaseUseCase reservationReleaseUseCase;

    public ReservationExpirationScheduler(ReservationReleaseUseCase reservationReleaseUseCase) {
        this.reservationReleaseUseCase = reservationReleaseUseCase;
    }

    @Scheduled(fixedDelayString = "${ticketing.expiration-check-interval-ms:30000}")
    public void releaseExpiredReservations() {
        reservationReleaseUseCase.releaseExpiredReservations()
            .doOnError(error -> LOGGER.error("Failed to release expired reservations", error))
            .subscribe();
    }
}
