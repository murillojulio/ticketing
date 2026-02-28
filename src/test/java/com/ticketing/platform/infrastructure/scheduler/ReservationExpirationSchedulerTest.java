package com.ticketing.platform.infrastructure.scheduler;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.port.in.ReservationReleaseUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationSchedulerTest {

    @Mock
    private ReservationReleaseUseCase reservationReleaseUseCase;

    @Test
    void shouldInvokeReleaseUseCaseOnScheduledExecution() {
        when(reservationReleaseUseCase.releaseExpiredReservations()).thenReturn(Mono.empty());
        ReservationExpirationScheduler scheduler = new ReservationExpirationScheduler(reservationReleaseUseCase);

        scheduler.releaseExpiredReservations();

        verify(reservationReleaseUseCase, times(1)).releaseExpiredReservations();
    }
}
