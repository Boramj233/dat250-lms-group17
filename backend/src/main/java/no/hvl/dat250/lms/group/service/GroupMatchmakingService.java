package no.hvl.dat250.lms.group.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.service.EnrollmentManagementService;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import no.hvl.dat250.lms.group.api.dto.GroupMatchmakingDTO;
import no.hvl.dat250.lms.group.api.dto.GroupMatchmakingDTO.Participation;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.domain.GroupMatchmaking;
import no.hvl.dat250.lms.group.domain.MatchmakingPreference;
import no.hvl.dat250.lms.group.repository.GroupMatchmakingRepository;
import no.hvl.dat250.lms.group.repository.GroupRepository;
import no.hvl.dat250.lms.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class GroupMatchmakingService {
    private final GroupMatchmakingRepository rounds;
    private final GroupRepository groups;
    private final EnrollmentService enrollments;
    private final EnrollmentManagementService enrollmentManagement;
    private final EntityManager entities;

    public GroupMatchmakingService(GroupMatchmakingRepository rounds, GroupRepository groups,
                                   EnrollmentService enrollments, EnrollmentManagementService enrollmentManagement,
                                   EntityManager entities) {
        this.rounds = rounds;
        this.groups = groups;
        this.enrollments = enrollments;
        this.enrollmentManagement = enrollmentManagement;
        this.entities = entities;
    }

    public GroupMatchmakingDTO create(Long courseId, Long userId, GroupMatchmakingDTO.CreateRequest request) {
        Course course = entities.find(Course.class, courseId, LockModeType.PESSIMISTIC_WRITE);
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        enrollments.lockActiveTeacher(userId, courseId);
        if (!Instant.now().isBefore(request.deadline())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deadline must be in the future");
        }
        if (rounds.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matchmaking already exists for this course");
        }
        GroupMatchmaking round = new GroupMatchmaking(course, request.deadline(), request.maxGroupSize());
        entities.persist(round);
        return response(round, userId);
    }

    @Transactional(readOnly = true)
    public GroupMatchmakingDTO get(Long courseId, Long userId) {
        return response(rounds.findById(courseId).orElseThrow(GroupMatchmakingService::missingRound), userId);
    }

    public GroupMatchmakingDTO submit(Long courseId, Long userId, MatchmakingPreference preference) {
        GroupMatchmaking round = rounds.lockByCourseId(courseId).orElseThrow(GroupMatchmakingService::missingRound);
        enrollments.lockActiveStudent(userId, courseId);
        if (round.isCompleted() || !Instant.now().isBefore(round.getDeadline())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matchmaking is closed");
        }
        round.submit(userId, preference);
        return response(round, userId);
    }

    @Transactional(readOnly = true)
    public List<Long> dueCourses() {
        return rounds.findDueCourses(Instant.now());
    }

    public void run(Long courseId) {
        GroupMatchmaking round = rounds.lockByCourseId(courseId).orElse(null);
        if (round == null || round.isCompleted() || Instant.now().isBefore(round.getDeadline())) {
            return;
        }

        Map<Long, MatchmakingPreference> preferences = round.preferences();
        List<Candidate> remaining = new ArrayList<>();
        for (Long userId : preferences.keySet().stream().sorted().toList()) {
            enrollments.findLockedActiveStudent(userId, courseId)
                    .filter(enrollment -> !groups.existsUserInCourseGroup(courseId, userId))
                    .ifPresent(enrollment -> remaining.add(new Candidate(enrollment.getUser(), preferences.get(userId))));
        }

        Set<Integer> usedNumbers = groups.findByCourseId(courseId).stream()
                .map(Group::getGroupNumber).collect(Collectors.toSet());
        int nextNumber = 1;
        int groupsLeft = Math.ceilDiv(remaining.size(), round.getMaxGroupSize());
        while (groupsLeft > 0) {
            while (usedNumbers.contains(nextNumber)) {
                nextNumber++;
            }
            int size = Math.ceilDiv(remaining.size(), groupsLeft);
            Group group = new Group(nextNumber++, round.getCourse(), round.getMaxGroupSize());
            List<Candidate> selected = new ArrayList<>();
            selected.add(remaining.removeFirst());
            while (selected.size() < size) {
                Candidate closest = remaining.stream()
                        .min(Comparator.comparingDouble(candidate -> candidate.distanceTo(selected)))
                        .orElseThrow();
                remaining.remove(closest);
                selected.add(closest);
            }
            selected.forEach(candidate -> group.addGroupMember(candidate.user()));
            groups.save(group);
            groupsLeft--;
        }

        round.complete();
    }

    private GroupMatchmakingDTO response(GroupMatchmaking round, Long userId) {
        Long courseId = round.getCourse().getId();
        CourseRole role = enrollments.activeCourseRoles(userId).get(courseId);
        Map<Long, MatchmakingPreference> preferences = round.preferences();
        Participation participation = switch (role) {
            case TEACHER -> {
                List<Enrollment> students = enrollmentManagement.members(courseId).stream()
                        .filter(enrollment -> enrollment.getRole() == CourseRole.STUDENT)
                        .toList();
                long submitted = students.stream()
                        .filter(enrollment -> preferences.containsKey(enrollment.getUser().getUserId()))
                        .count();
                yield new Participation(submitted, students.size());
            }
            case STUDENT -> null;
            case null -> throw new AccessDeniedException("Active course enrollment required");
        };
        return new GroupMatchmakingDTO(round.getDeadline(), round.getMaxGroupSize(),
                !round.isCompleted() && Instant.now().isBefore(round.getDeadline()), round.isCompleted(),
                preferences.get(userId), participation);
    }

    private static ResponseStatusException missingRound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Matchmaking has not been created");
    }

    private record Candidate(User user, MatchmakingPreference preference) {
        double distanceTo(List<Candidate> group) {
            return group.stream().mapToDouble(member -> preference.distanceTo(member.preference())).sum();
        }
    }
}
