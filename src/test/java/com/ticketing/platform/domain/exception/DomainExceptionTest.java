package com.ticketing.platform.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    void shouldKeepCauseWhenConstructedWithMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("root");

        DomainException exception = new DomainException("wrapper", cause);

        assertThat(exception.getMessage()).isEqualTo("wrapper");
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
