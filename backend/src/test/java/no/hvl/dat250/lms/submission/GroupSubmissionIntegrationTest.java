package no.hvl.dat250.lms.submission;

import java.math.BigDecimal;
import java.util.*;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.course.domain.*;
import no.hvl.dat250.lms.enrollment.domain.*;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.service.GroupService;
import no.hvl.dat250.lms.grade.service.*;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.*;
import no.hvl.dat250.lms.submission.service.*;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:group_submission_it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GroupSubmissionIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @Autowired SubmissionService submissions;
    @Autowired GroupService groups;
    @Autowired GradingService grades;
    @Autowired MockMvc mvc;
    @Autowired FileSubmissionService files;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage storage;

    private <T> T save(T entity) { em.persist(entity); em.flush(); return entity; }
    private <T> T tx(java.util.function.Supplier<T> action) {
        return new TransactionTemplate(tm).execute(status -> action.get());
    }
    private User user() { return save(new User(UUID.randomUUID()+"@example.com", "Test", "Student")); }
    record Fixture(Long course, Long content, Long group, Long a, Long b, Long c, Long teacher, Long bEnrollment) {}
    private Fixture fixture() {
        return fixture(false);
    }
    private Fixture fixture(boolean coding) {
        return tx(() -> {
            var course = save(new Course("DAT250", "Groups", Term.AUTUMN, 2026));
            var a = user(); var b = user(); var c = user(); var teacher = user();
            save(new Enrollment(a, course, CourseRole.STUDENT));
            var eb = save(new Enrollment(b, course, CourseRole.STUDENT));
            save(new Enrollment(c, course, CourseRole.STUDENT));
            save(new Enrollment(teacher, course, CourseRole.TEACHER));
            var module = save(new no.hvl.dat250.lms.module.domain.Module(course, "Week", 0));
            Content content = coding
                ? new CodingExercise(module, "Code", 0, "Implement", "https://example.com/starter.git", null, false)
                : new Assignment(module, "Report", 0, "Write", null, false);
            save(content);
            var group = new Group(1, course);
            group.addGroupMember(a); group.addGroupMember(b); save(group);
            return new Fixture(course.getId(), content.getId(), group.getGroupId(), a.getUserId(),
                b.getUserId(), c.getUserId(), teacher.getUserId(), eb.getId());
        });
    }
    private Long submit(Fixture f, Long user) {
        return submissions.submit(f.content, user, SubmissionType.TEXT, "Group answer", f.group).getId();
    }
    private String token(Long userId) {
        return tx(() -> TestTokens.bearer(em.find(User.class, userId)));
    }
    private void getAs(String path, Long user, int status) throws Exception {
        mvc.perform(get(path).header("Authorization", token(user)))
            .andExpect(status().is(status));
    }

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void historicalSnapshotSurvivesMembershipChangesAndWithdrawal(boolean coding) throws Exception {
        var f = fixture(coding); Long first = submit(f, f.a);
        var grade = grades.save(first, f.teacher, BigDecimal.TEN, "Feedback", null, true);
        grades.release(first, f.teacher, grade.version());
        groups.removeGroupMember(f.course, f.group, f.b);
        groups.addGroupMember(f.course, f.group, f.c);
        tx(() -> { em.find(Enrollment.class, f.bEnrollment).deactivate(); return null; });
        Long second = submit(f, f.c);
        for (Long owner : List.of(f.a, f.b)) {
            getAs("/api/submissions/"+first, owner, 200);
            getAs("/api/submissions/"+first+"/grade", owner, 200);
        }
        getAs("/api/submissions/"+first, f.c, 404);
        getAs("/api/submissions/"+first+"/grade", f.c, 404);
        getAs("/api/submissions/"+second, f.b, 404);
        getAs("/api/submissions/"+second, f.c, 200);
        mvc.perform(get("/api/contents/"+f.content+"/submissions/me")
            .header("Authorization", token(f.b)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(first));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> groups.deleteGroup(f.course, f.group));
        getAs("/api/submissions/"+first, f.b, 200);
    }

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void latestAndHistoryUseGroupRatherThanSubmitterAndSeparateIndividuals(boolean coding) {
        var f = fixture(coding); Long first = submit(f, f.a);
        Long second = submit(f, f.b);
        submissions.submitIndividual(f.content, f.a, SubmissionType.TEXT, "Personal");
        assertThrows(GradeConflictException.class,
            () -> grades.save(first, f.teacher, BigDecimal.TEN, null, null));
        var grade = grades.save(second, f.teacher, BigDecimal.TEN, null, null, true);
        grades.release(second, f.teacher, grade.version());
        assertEquals(2, submissions.reviewLatest(f.content, f.teacher, 0, 20).getTotalElements());
        assertEquals(List.of(second, first), submissions.reviewHistory(first, f.teacher).stream().map(r -> r.id()).toList());
        assertEquals(BigDecimal.TEN.setScale(2), grades.ownReleased(second, f.a).score());
        assertEquals(BigDecimal.TEN.setScale(2), grades.ownReleased(second, f.b).score());
    }

    @Test void authorizationRejectsNonMembersInactiveMembersAndCrossCourse() throws Exception {
        var f = fixture();
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> submit(f, f.c));
        tx(() -> { em.find(Enrollment.class, f.bEnrollment).deactivate(); return null; });
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> submit(f, f.a));
        mvc.perform(post("/api/contents/"+f.content+"/submissions")
            .contentType("application/json").content("{\"type\":\"TEXT\",\"payload\":\"answer\",\"groupId\":"+f.group+"}"))
            .andExpect(status().isUnauthorized());
        var other = fixture();
        mvc.perform(post("/api/contents/"+other.content+"/submissions")
            .header("Authorization", token(other.a))
            .contentType("application/json").content("{\"type\":\"TEXT\",\"payload\":\"answer\",\"groupId\":"+f.group+"}"))
            .andExpect(status().isNotFound());
    }

    @Test void groupManagementRequiresCourseTeacherAndValidStudent() throws Exception {
        var f = fixture();
        mvc.perform(post("/api/courses/"+f.course+"/groups/"+f.group+"/members")
            .header("Authorization", token(f.a))
            .contentType("application/json").content(f.c.toString())).andExpect(status().isForbidden());
        mvc.perform(post("/api/courses/"+f.course+"/groups/"+f.group+"/members")
            .header("Authorization", token(f.teacher))
            .contentType("application/json").content(f.teacher.toString())).andExpect(status().isForbidden());
        mvc.perform(post("/api/courses/"+f.course+"/groups/"+f.group+"/members")
            .header("Authorization", token(f.teacher))
            .contentType("application/json").content(f.c.toString())).andExpect(status().isOk());
        var other = fixture();
        getAs("/api/courses/"+f.course+"/groups", other.a, 403);
        mvc.perform(get("/api/courses/"+f.course+"/groups")).andExpect(status().isUnauthorized());
    }

    @Test void groupJsonResponseIncludesActualSubmitterAndSnapshot() throws Exception {
        var f = fixture();
        mvc.perform(post("/api/contents/"+f.content+"/submissions")
            .header("Authorization", token(f.a))
            .contentType("application/json").content("{\"type\":\"TEXT\",\"payload\":\"answer\",\"groupId\":"+f.group+"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.groupId").value(f.group))
            .andExpect(jsonPath("$.submittedByUserId").value(f.a))
            .andExpect(jsonPath("$.members.length()").value(2));
    }

    @Test void fileGetAndHeadUseSnapshotAndDoNotAccessStorageForOutsider() throws Exception {
        var f = fixture();
        org.mockito.Mockito.when(storage.get(org.mockito.ArgumentMatchers.any()))
            .thenAnswer(call -> new java.io.ByteArrayInputStream("%PDF-test".getBytes()));
        Long id = files.submitFile(f.content, f.a,
            new org.springframework.mock.web.MockMultipartFile("file", "report.pdf", "application/pdf", "%PDF-test".getBytes()), f.group).id();
        groups.removeGroupMember(f.course, f.group, f.b);
        groups.addGroupMember(f.course, f.group, f.c);
        org.mockito.Mockito.clearInvocations(storage);
        getAs("/api/submissions/"+id+"/file", f.c, 404);
        mvc.perform(head("/api/submissions/"+id+"/file")
            .header("Authorization", token(f.c))).andExpect(status().isNotFound());
        org.mockito.Mockito.verifyNoInteractions(storage);
        getAs("/api/submissions/"+id+"/file", f.b, 200);
        mvc.perform(head("/api/submissions/"+id+"/file")
            .header("Authorization", token(f.b))).andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    @Test void membershipChangeDuringUploadRechecksAndCompensates() {
        var f = fixture();
        org.mockito.Mockito.doAnswer(call -> {
            groups.removeGroupMember(f.course, f.group, f.a);
            return new no.hvl.dat250.lms.submission.storage.SubmissionObjectStorage.ObjectReference("test", "key");
        }).when(storage).put(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
            files.submitFile(f.content, f.a, new org.springframework.mock.web.MockMultipartFile(
                "file", "report.pdf", "application/pdf", "%PDF-test".getBytes()), f.group));
        org.mockito.Mockito.verify(storage).delete(org.mockito.ArgumentMatchers.any());
        assertTrue(submissions.findMySubmissionHistory(f.content, f.b).isEmpty());
    }
    @Test void refreshingLockedGroupDiscardsPreviouslyLoadedMembers() {
        var f = fixture();
        tx(() -> {
            var cached = em.find(Group.class, f.group);
            assertEquals(2, cached.getGroupMembers().size());
            var independent = new TransactionTemplate(tm);
            independent.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            independent.executeWithoutResult(status -> groups.removeGroupMember(f.course, f.group, f.b));
            var locked = groups.lockGroup(f.course, f.group);
            assertEquals(Set.of(f.a), locked.getGroupMembers().stream()
                .map(User::getUserId).collect(java.util.stream.Collectors.toSet()));
            return null;
        });
    }

    @Test void groupLockSerializesSubmissionAndMembershipRemoval() throws Exception {
        var f = fixture();
        var locked = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var acceptance = pool.submit(() -> tx(() -> {
                groups.lockGroup(f.course, f.group);
                locked.countDown();
                try { assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS)); }
                catch (InterruptedException ex) { throw new RuntimeException(ex); }
                return submit(f, f.a);
            }));
            assertTrue(locked.await(5, java.util.concurrent.TimeUnit.SECONDS));
            var started = new java.util.concurrent.CountDownLatch(1);
            var removal = pool.submit(() -> {
                started.countDown();
                groups.removeGroupMember(f.course, f.group, f.b);
            });
            assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
            try {
                assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> removal.get(300, java.util.concurrent.TimeUnit.MILLISECONDS));
            } finally { release.countDown(); }
            Long id = acceptance.get(5, java.util.concurrent.TimeUnit.SECONDS);
            removal.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(2, submissions.findOwnSubmission(id, f.b).getMembers().size());
        } finally { release.countDown(); }
    }

    @ParameterizedTest(name = "coding={0}")
    @ValueSource(booleans = {false, true})
    void resubmissionAndFirstReleaseShareGroupLock(boolean coding) throws Exception {
        var f = fixture(coding); Long first = submit(f, f.a);
        var grade = grades.save(first, f.teacher, BigDecimal.TEN, null, null, true);
        var locked = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var resubmit = pool.submit(() -> tx(() -> {
                groups.lockGroup(f.course, f.group);
                locked.countDown();
                try { assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS)); }
                catch (InterruptedException ex) { throw new RuntimeException(ex); }
                return submit(f, f.b);
            }));
            assertTrue(locked.await(5, java.util.concurrent.TimeUnit.SECONDS));
            var publishing = pool.submit(() -> grades.release(first, f.teacher, grade.version()));
            try {
                assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> publishing.get(300, java.util.concurrent.TimeUnit.MILLISECONDS));
            } finally { release.countDown(); }
            resubmit.get(5, java.util.concurrent.TimeUnit.SECONDS);
            var failure = assertThrows(java.util.concurrent.ExecutionException.class,
                () -> publishing.get(5, java.util.concurrent.TimeUnit.SECONDS));
            assertInstanceOf(GradeConflictException.class, failure.getCause());
            assertThrows(NoSuchElementException.class, () -> grades.ownReleased(first, f.a));
        } finally { release.countDown(); }
    }

    @Test void concurrentAddsCannotPlaceStudentInTwoGroups() throws Exception {
        var f = fixture();
        Long otherGroup = groups.createGroup(f.course, 2).id();
        var gate = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.function.Function<Long, java.util.concurrent.Callable<Boolean>> add = groupId -> () -> {
                assertTrue(gate.await(5, java.util.concurrent.TimeUnit.SECONDS));
                try { groups.addGroupMember(f.course, groupId, f.c); return true; }
                catch (org.springframework.web.server.ResponseStatusException ex) {
                    assertEquals(409, ex.getStatusCode().value()); return false;
                }
            };
            var one = pool.submit(add.apply(f.group));
            var two = pool.submit(add.apply(otherGroup));
            gate.countDown();
            assertNotEquals(one.get(5, java.util.concurrent.TimeUnit.SECONDS),
                two.get(5, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(1, groups.getAllGroups(f.course).stream().filter(g -> g.memberIds().contains(f.c)).count());
    }

    @Test void codingExerciseGroupGitSubmissionCanBeManuallyGraded() throws Exception {
        var f = fixture();
        Long codingId = tx(() -> {
            var assignment = em.find(Assignment.class, f.content);
            return save(new CodingExercise(assignment.getModule(), "Code", 1, "Implement",
                "https://example.com/starter.git", null, false)).getId();
        });
        Long id = submissions.submit(codingId, f.b, SubmissionType.GIT_URL,
            "https://example.com/answer.git", f.group).getId();
        assertEquals(2, submissions.findOwnSubmission(id, f.a).getMembers().size());
        mvc.perform(get("/api/submissions/{id}/review", id)
                .header("Authorization", token(f.teacher)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("GIT_URL"))
            .andExpect(jsonPath("$.members.length()").value(2));
        mvc.perform(put("/api/submissions/{id}/grade", id)
                .header("Authorization", token(f.teacher)).contentType("application/json")
                .content("{\"score\":75,\"feedback\":\"Good solution\",\"passed\":true}"))
            .andExpect(status().isOk());
        getAs("/api/submissions/" + id + "/grade", f.a, 404);
        getAs("/api/submissions/" + id + "/grade", f.b, 404);
        mvc.perform(post("/api/submissions/{id}/grade/release", id)
                .header("Authorization", token(f.teacher)).contentType("application/json")
                .content("{\"version\":0}"))
            .andExpect(status().isOk());
        for (Long member : List.of(f.a, f.b)) {
            mvc.perform(get("/api/submissions/{id}/grade", id).header("Authorization", token(member)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.score").value(75))
                .andExpect(jsonPath("$.feedback").value("Good solution"))
                .andExpect(jsonPath("$.passed").value(true));
        }
        getAs("/api/submissions/" + id + "/grade", f.c, 404);
        org.mockito.Mockito.clearInvocations(storage);
        getAs("/api/submissions/" + id + "/review/file", f.teacher, 404);
        mvc.perform(head("/api/submissions/{id}/review/file", id)
            .header("Authorization", token(f.teacher))).andExpect(status().isNotFound());
        org.mockito.Mockito.verifyNoInteractions(storage);
    }

}
