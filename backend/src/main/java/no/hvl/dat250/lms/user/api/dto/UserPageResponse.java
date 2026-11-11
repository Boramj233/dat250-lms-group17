package no.hvl.dat250.lms.user.api.dto;

import java.util.List;
import no.hvl.dat250.lms.user.domain.User;
import org.springframework.data.domain.Page;

/** A page of profiles for administrators. */
public record UserPageResponse(List<UserResponse> users, int page, int size,
                               long totalElements, int totalPages) {
    public static UserPageResponse from(Page<User> page) {
        return new UserPageResponse(page.getContent().stream().map(UserResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
