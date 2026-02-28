package com.ticketing.platform.infrastructure.time;

import com.ticketing.platform.application.port.out.ClockPort;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SystemClockAdapter implements ClockPort {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
