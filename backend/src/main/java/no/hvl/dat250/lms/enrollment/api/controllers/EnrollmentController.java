package no.hvl.dat250.lms.enrollment.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;
import jakarta.validation.Valid;
import no.hvl.dat250.lms.enrollment.api.dto.EnrollRequest;
import no.hvl.dat250.lms.enrollment.api.dto.EnrollmentResponse;
import no.hvl.dat250.lms.enrollment.api.dto.UpdateCourseRoleRequest;
import no.hvl.dat250.lms.enrollment.service.EnrollmentAlreadyExistsException;
import no.hvl.dat250.lms.enrollment.service.EnrollmentManagementService;
import no.hvl.dat250.lms.user.service.UserNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * A course's members. Everyone enrolled in the course (and admins) can see who is in
 * it; only teachers of the course and admins can change it.
 */
@RestController
@RequestMapping("/api/courses/{courseId}/enrollments")
public class EnrollmentController {
    /** Changing members: a teacher of this course, or an admin. */
    private static final String TEACHER_OR_ADMIN =
        "hasRole('ADMIN') or @courseAccess.isTeacher(authentication, #courseId)";

    private final EnrollmentManagementService enrollments;

    public EnrollmentController(EnrollmentManagementService enrollments) {
        this.enrollments = enrollments;
    }

    /** The course's active members, sorted by name. */
    @PreAuthorize("hasRole('ADMIN') or @courseAccess.isEnrolled(authentication, #courseId)")
    @GetMapping
    public List<EnrollmentResponse> getMembers(@PathVariable Long courseId) {
        return enrollments.members(courseId).stream().map(EnrollmentResponse::from).toList();
    }

    /** Adds a user to the course (or back to it); 409 if they are already a member. */
    @PreAuthorize(TEACHER_OR_ADMIN)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@PathVariable Long courseId, @Valid @RequestBody EnrollRequest request) {
        return EnrollmentResponse.from(enrollments.enroll(courseId, request.userId(), request.role()));
    }

    /** Makes a member a teacher or a student. */
    @PreAuthorize(TEACHER_OR_ADMIN)
    @PatchMapping("/{userId}")
    public EnrollmentResponse changeRole(@PathVariable Long courseId, @PathVariable Long userId,
                                         @Valid @RequestBody UpdateCourseRoleRequest request) {
        return EnrollmentResponse.from(enrollments.changeRole(courseId, userId, request.role()));
    }

    /** Removes a member; their submissions and grades are kept. */
    @PreAuthorize(TEACHER_OR_ADMIN)
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long courseId, @PathVariable Long userId) {
        enrollments.remove(courseId, userId);
    }

    @ExceptionHandler({NoSuchElementException.class, UserNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void notFound() {
    }

    @ExceptionHandler({EnrollmentAlreadyExistsException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    void conflict() {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    void badRequest() {
    }
}
