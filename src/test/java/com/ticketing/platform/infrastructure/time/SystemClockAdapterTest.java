package com.ticketing.platform.infrastructure.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SystemClockAdapterTest {

    @Test
    void shouldReturnCurrentInstant() {
        SystemClockAdapter adapter = new SystemClockAdapter();

        Instant now = adapter.now();

        assertThat(now).isNotNull();
    }
}
