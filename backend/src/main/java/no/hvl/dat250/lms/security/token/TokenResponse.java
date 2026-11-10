package no.hvl.dat250.lms.security.token;

import java.time.Instant;

/** Body of GET /token: the API token for the logged-in browser session. */
public record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {}
