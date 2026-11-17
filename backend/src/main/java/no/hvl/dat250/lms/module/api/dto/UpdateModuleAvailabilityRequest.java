package no.hvl.dat250.lms.module.api.dto;

import java.time.LocalDateTime;

public record UpdateModuleAvailabilityRequest(
        LocalDateTime availableFrom,
        LocalDateTime availableToAllFrom
) {}