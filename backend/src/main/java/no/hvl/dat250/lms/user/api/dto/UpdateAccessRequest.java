package no.hvl.dat250.lms.user.api.dto;

import jakarta.validation.constraints.NotNull;
import no.hvl.dat250.lms.user.domain.Access;

/** Global access is edited separately from ordinary profile fields. */
public record UpdateAccessRequest(@NotNull Access access) {}
