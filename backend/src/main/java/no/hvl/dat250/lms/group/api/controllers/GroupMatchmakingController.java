package no.hvl.dat250.lms.group.api.controllers;

import jakarta.validation.Valid;
import no.hvl.dat250.lms.group.api.dto.GroupMatchmakingDTO;
import no.hvl.dat250.lms.group.service.GroupMatchmakingService;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.security.auth.annotations.IsEnrolledInCourse;
import no.hvl.dat250.lms.security.auth.annotations.IsStudentInCourse;
import no.hvl.dat250.lms.security.auth.annotations.IsTeacherInCourse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/groups/matchmaking")
public class GroupMatchmakingController {
    private final GroupMatchmakingService matchmaking;

    public GroupMatchmakingController(GroupMatchmakingService matchmaking) {
        this.matchmaking = matchmaking;
    }

    @IsTeacherInCourse
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMatchmakingDTO create(
            @PathVariable Long courseId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody GroupMatchmakingDTO.CreateRequest request
    ) {
        return matchmaking.create(courseId, user.userId(), request);
    }

    @IsEnrolledInCourse
    @GetMapping
    public GroupMatchmakingDTO get(@PathVariable Long courseId, @AuthenticationPrincipal AuthenticatedUser user) {
        return matchmaking.get(courseId, user.userId());
    }

    @IsStudentInCourse
    @PutMapping("/preferences")
    public GroupMatchmakingDTO submit(
            @PathVariable Long courseId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody GroupMatchmakingDTO.PreferenceRequest request
    ) {
        return matchmaking.submit(courseId, user.userId(), request.toPreference());
    }
}
