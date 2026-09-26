package com.sidis.aircraftservice.exceptions;

/**
 * Base class for all domain rule violations in this application.
 *
 * <p>Subclass this when a domain invariant is broken (AircraftAvailability.g. a copy is not available, a book is
 * not found). Using a hierarchy of domain exceptions — rather than Java built-ins like
 * {@link IllegalArgumentException} — allows the {@link GlobalExceptionHandler} to map each
 * exception type to the correct HTTP status and provides meaningful names that reflect the
 * ubiquitous language.</p>
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
