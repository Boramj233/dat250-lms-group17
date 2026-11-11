package no.hvl.dat250.lms.user.api.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Paginated account choices, without global access or authentication state. */
public record UserLookupPageResponse(List<UserLookupResponse> users, int page, int size,
                                     long totalElements, int totalPages) {
    public static UserLookupPageResponse from(Page<UserLookupResponse> page) {
        return new UserLookupPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
