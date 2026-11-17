package no.hvl.dat250.lms.submission.domain;

import java.time.Instant;
import java.util.UUID;

/** Immutable event data; no entities, answer content or storage coordinates. */
public record SubmissionAccepted(
        UUID eventId,
        Long submissionId,
        Long contentId,
        Instant submittedAt,
        boolean late) {}
