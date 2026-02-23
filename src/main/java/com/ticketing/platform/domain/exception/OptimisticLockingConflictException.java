package com.ticketing.platform.domain.exception;

public class OptimisticLockingConflictException extends DomainException {

    public OptimisticLockingConflictException(String message) {
        super(message);
    }
}
