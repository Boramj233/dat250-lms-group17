package no.hvl.dat250.lms.content.api.dto;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** Start keeps its original PUT semantics; an omitted end preserves the existing end. */
public class UpdateContentVisibilityRequest {
    private Instant visibleFrom;
    private Instant visibleUntil;
    @JsonIgnore
    private boolean visibleUntilProvided;

    public Instant getVisibleFrom() { return visibleFrom; }
    public void setVisibleFrom(Instant visibleFrom) { this.visibleFrom = visibleFrom; }

    public Instant getVisibleUntil() { return visibleUntil; }
    public void setVisibleUntil(Instant visibleUntil) {
        this.visibleUntil = visibleUntil;
        this.visibleUntilProvided = true;
    }

    // Jackson calls the setter for explicit null, but not for an omitted property.
    public boolean hasVisibleUntil() { return visibleUntilProvided; }
}
