package com.ticketing.platform.application.port.out;

import java.time.Instant;

public interface ClockPort {

    Instant now();
}
