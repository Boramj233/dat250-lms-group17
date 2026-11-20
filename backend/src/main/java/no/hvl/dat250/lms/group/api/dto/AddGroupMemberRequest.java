package no.hvl.dat250.lms.group.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;

public record AddGroupMemberRequest(@NotNull Long userId) {

    // Keep accepting a bare user ID in the JSON request body.
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public AddGroupMemberRequest {}
}
