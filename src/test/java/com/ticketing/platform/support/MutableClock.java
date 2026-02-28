package com.ticketing.platform.support;

import com.ticketing.platform.application.port.out.ClockPort;
import java.time.Instant;

public class MutableClock implements ClockPort {

    private Instant current;

    public MutableClock(Instant current) {
        this.current = current;
    }

    @Override
    public Instant now() {
        return current;
    }

    public void setCurrent(Instant current) {
        this.current = current;
    }
}
